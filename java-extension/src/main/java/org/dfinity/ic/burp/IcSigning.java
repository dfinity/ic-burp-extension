package org.dfinity.ic.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.dfinity.ic.burp.IcBurpExtension.*;
import static org.dfinity.ic.burp.IcHttpRequestResponseViewer.getCanisterId;

public class IcSigning implements HttpHandler {
    private final Logging log;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final InternetIdentities internetIdentities;
    private final TopPanel topPanel;

    public IcSigning(Logging log, IcTools icTools, TopPanel topPanel, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, InternetIdentities internetIdentities) {
        this.log = log;
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.internetIdentities = internetIdentities;
        this.topPanel = topPanel;
    }

    /**
     * Takes the request to be sent and verifies if this is an IC API request that needs re-encoding and re-signing.
     * Fetches the correct identity from the x-ic-sign-identity header. This can be 'anonymous' or an anchor. The
     * corresponding identity is used to sign the request or an error pop-up is generated.
     *
     * @param requestToBeSent information about the HTTP request that is going to be sent.
     *
     * @return Returns the original request except for decoded IC API requests where the original request is re-encoded
     * and re-signed. Note that in an error case, the function will return null. The behaviour of BurpSuite in case of
     * a null response, is to send the original request as if this handler never ran. There is no way to 'cancel' a request.
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if(!requestToBeSent.toolSource().isFromTool(ToolType.REPEATER) && !requestToBeSent.toolSource().isFromTool(ToolType.INTRUDER)) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        HttpHeader decodedHeader = requestToBeSent.header(IC_DECODED_HEADER_NAME);
        if(decodedHeader == null || !decodedHeader.value().trim().equalsIgnoreCase("true"))
            return RequestToBeSentAction.continueWith(requestToBeSent);

        Optional<String> cid = getCanisterId(requestToBeSent.path());
        Optional<String> idlopt = Optional.empty();
        if (cid.isPresent()) {
            CompletableFuture<CanisterCacheInfo> info = this.canisterInterfaceCache.getIfPresent(cid.get());
            if (info != null)
                idlopt = info.join().getActiveCanisterInterface();
        }

        try {
            String anchor = requestToBeSent.header(IC_SIGN_IDENTITY_HEADER_NAME).value();
            if(anchor.isBlank()){
                topPanel.showErrorMessage("The message needs to have an anchor set in the " + IC_SIGN_IDENTITY_HEADER_NAME + " header.", "IC error.");
                this.log.logToError("No anchor set in " + IC_SIGN_IDENTITY_HEADER_NAME + " header.");
                return null;
            }
            // Take the icOrigin header if present and otherwise the normal origin one as backup.
            // The icOrigin header is set when decoding the request and finding the principal from an earlier call to get_delegation.
            Optional<String> icOrigin = Optional.ofNullable(requestToBeSent.headerValue(IC_ORIGIN_HEADER_NAME));
            String origin = icOrigin.orElse(requestToBeSent.headerValue("Origin"));
            if(origin.isBlank()) {
                topPanel.showErrorMessage("The origin header needs to be set to match the origin of the dApp.", "IC error.");
                this.log.logToError("Empty Origin header for proxy request to " + requestToBeSent.url());
                return null;
            }

            Optional<Identity> id = internetIdentities.findSignIdentity(anchor, origin);
            if(id.isEmpty()){
                topPanel.showErrorMessage("Could not retrieve a valid identity.", "IC error.");
                this.log.logToError("Error retrieving signIdentity for anchor " + anchor + " and request URI " + requestToBeSent.url());
                return null;
            }

            byte[] newBody = icTools.encodeAndSignCanisterRequest(requestToBeSent.bodyToString(), idlopt, id.get());
            HttpRequest req = requestToBeSent.withRemovedHeader(IC_SIGN_IDENTITY_HEADER_NAME);
            req = req.withRemovedHeader(IC_DECODED_HEADER_NAME);
            req = req.withRemovedHeader(IC_ORIGIN_HEADER_NAME);
            req = req.withBody(ByteArray.byteArray(newBody));
            return RequestToBeSentAction.continueWith(req);
        } catch (IcToolsException e) {
            this.log.logToError("Exception raised during re-signing of the request: " + e);
            return null;
        }
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {

        HttpRequest request = responseReceived.initiatingRequest();

        HttpHeader content_type = responseReceived.header("Content-Type");
        if (content_type == null || !content_type.value().equals("application/cbor")) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        Optional<String> cidOpt = getCanisterId(request.path());
        if (cidOpt.isEmpty()) {
            // This does not look like an IC request. We don't decode anything.
            return ResponseReceivedAction.continueWith(responseReceived);
        }


        if (!responseReceived.toolSource().isFromTool(ToolType.REPEATER) && !responseReceived.toolSource().isFromTool(ToolType.INTRUDER)) {
            // Currently only the requests that come from the repeater and intruder are decoded as these are the only tools that support request modification.
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        String cid = cidOpt.orElseThrow(() -> new RuntimeException("Canister id not present in " + request));
        CanisterCacheInfo canisterCacheInfo = canisterInterfaceCache.get(cid).join();

        Optional<String> canisterInterface = canisterCacheInfo == null ? Optional.empty() : canisterCacheInfo.getActiveCanisterInterface();
        String newBody;
        try {
            Optional<CanisterInterfaceInfo> canisterInterfaceInfo;
            if (canisterInterface.isPresent()) {
                var metadata = icTools.getRequestMetadata(request.body().getBytes());
                canisterInterfaceInfo = metadata.canisterMethod().map(m -> new CanisterInterfaceInfo(canisterInterface.get(), m));
            } else {
                canisterInterfaceInfo = Optional.empty();
            }
            newBody = icTools.decodeCanisterResponse(responseReceived.body().getBytes(), canisterInterfaceInfo);
        } catch (IcToolsException e) {
            topPanel.showErrorMessage("Could not decode response with path " + request.path() +
                    "\nThis could be due to a malformed response or due to an issue with the IDL.", "IC Decoding error");
            log.logToError("Failed to decode response with path " + request.path(), e);
            newBody = String.format("Failed to decode response with path %s: %s", request.path(), e.getStackTraceAsString());
        }
        return ResponseReceivedAction.continueWith(responseReceived.withAddedHeader(IC_DECODED_HEADER_NAME, "True").withBody(newBody));
    }
}

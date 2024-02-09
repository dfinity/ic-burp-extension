package org.dfinity.ic.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.UrlPathInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.RequestEncoded;
import org.dfinity.ic.burp.tools.model.RequestMetadata;
import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.dfinity.ic.burp.IcBurpExtension.IC_DECODED_HEADER_NAME;
import static org.dfinity.ic.burp.IcBurpExtension.IC_FRONTEND_HOSTNAME_HEADER_NAME;
import static org.dfinity.ic.burp.IcBurpExtension.IC_SIGN_IDENTITY_HEADER_NAME;

public class IcSigning implements HttpHandler {
    private final Logging log;
    private final IcTools icTools;
    private final InternetIdentities internetIdentities;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Cache<String, Identity> callRequestSignIdentityCache;
    private final TopPanel topPanel;

    public IcSigning(Logging log, IcTools icTools, TopPanel topPanel, InternetIdentities internetIdentities, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache) {
        this.log = log;
        this.icTools = icTools;
        this.topPanel = topPanel;
        this.internetIdentities = internetIdentities;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.callRequestSignIdentityCache = Caffeine.newBuilder().maximumSize(10_000).build();
    }

    /**
     * Takes the request to be sent and verifies if this is an IC API request that needs re-encoding and re-signing.
     * Fetches the correct identity from the x-ic-sign-identity header. This can be 'anonymous' or an anchor. The
     * corresponding identity is used to sign the request or an error pop-up is generated.
     *
     * @param requestToBeSent information about the HTTP request that is going to be sent.
     * @return Returns the original request except for decoded IC API requests where the original request is re-encoded
     * and re-signed. Note that in an error case, the function will return null. The behaviour of BurpSuite in case of
     * a null response, is to send the original request as if this handler never ran. There is no way to 'cancel' a request.
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (!requestToBeSent.toolSource().isFromTool(ToolType.REPEATER) && !requestToBeSent.toolSource().isFromTool(ToolType.INTRUDER)) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        HttpHeader decodedHeader = requestToBeSent.header(IC_DECODED_HEADER_NAME);
        if (decodedHeader == null || !decodedHeader.value().trim().equalsIgnoreCase("true"))
            return RequestToBeSentAction.continueWith(requestToBeSent);

        UrlPathInfo urlPathInfo = UrlPathInfo.tryFrom(requestToBeSent.path()).orElse(null);
        if (urlPathInfo == null)
            return RequestToBeSentAction.continueWith(requestToBeSent);

        Optional<String> idlopt = Optional.empty();
        CompletableFuture<CanisterCacheInfo> info = this.canisterInterfaceCache.getIfPresent(urlPathInfo.canisterId());
        if (info != null)
            idlopt = info.join().getActiveCanisterInterface();

        try {
            String anchor = requestToBeSent.header(IC_SIGN_IDENTITY_HEADER_NAME).value();
            if (anchor.isBlank()) {
                topPanel.showErrorMessage("The message needs to have an anchor set in the " + IC_SIGN_IDENTITY_HEADER_NAME + " header.", "IC error");
                this.log.logToError("No anchor set in " + IC_SIGN_IDENTITY_HEADER_NAME + " header.");
                return null;
            }
            // Take the IC_FRONTEND_HOSTNAME_HEADER_NAME header if present and otherwise the origin is used one as backup.
            // The IC_FRONTEND_HOSTNAME_HEADER_NAME header is set when decoding the request and finding the principal from an earlier call to get_delegation.
            Optional<String> icFrontendHostname = Optional.ofNullable(requestToBeSent.headerValue(IC_FRONTEND_HOSTNAME_HEADER_NAME));
            String frontendHostname = icFrontendHostname.orElse(requestToBeSent.headerValue("Origin"));
            if (frontendHostname.isBlank()) {
                topPanel.showErrorMessage("The the outgoing request requires either a valid origin header and/or the "
                        + IC_FRONTEND_HOSTNAME_HEADER_NAME + "header needs to be set to match the frontendHostname used by the " +
                        "dApp for it's internet identity.", "IC error");
                this.log.logToError("Empty Origin and " + IC_FRONTEND_HOSTNAME_HEADER_NAME + " header for proxy request to " + requestToBeSent.url());
                return null;
            }

            Optional<Identity> id = internetIdentities.findSignIdentity(anchor, frontendHostname);
            if (id.isEmpty()) {
                topPanel.showErrorMessage("Could not retrieve a valid identity. \nIs this II onboarded and still active or has the passkey been removed?", "IC error");
                this.log.logToError("Error retrieving signIdentity for anchor " + anchor + " and request URI " + requestToBeSent.url());
                return null;
            }

            RequestEncoded request = icTools.encodeAndSignCanisterRequest(requestToBeSent.bodyToString(), idlopt, id.get());
            HttpRequest req = requestToBeSent.withRemovedHeader(IC_SIGN_IDENTITY_HEADER_NAME);
            req = req.withRemovedHeader(IC_DECODED_HEADER_NAME);
            req = req.withRemovedHeader(IC_FRONTEND_HOSTNAME_HEADER_NAME);
            req = req.withBody(ByteArray.byteArray(request.encodedBody()));

            if (urlPathInfo.requestType() == RequestType.CALL && request.type() == RequestType.CALL && request.requestId().isPresent()) {
                callRequestSignIdentityCache.put(request.requestId().get(), id.get());
            }
            return RequestToBeSentAction.continueWith(req);
        } catch (IcToolsException e) {
            topPanel.showErrorMessage("Encoding or re-signing the request failed. Sending the decoded version as fallback.", "IC error");
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

        UrlPathInfo urlPathInfo = UrlPathInfo.tryFrom(request.path()).orElse(null);
        if (urlPathInfo == null) {
            // This does not look like an IC request. We don't decode anything.
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        if (!responseReceived.toolSource().isFromTool(ToolType.REPEATER) && !responseReceived.toolSource().isFromTool(ToolType.INTRUDER)) {
            // Currently only the requests that come from the repeater and intruder are decoded as these are the only tools that support request modification.
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        String cid = urlPathInfo.canisterId();
        CanisterCacheInfo canisterCacheInfo = canisterInterfaceCache.get(cid).join();

        Optional<String> canisterInterface = canisterCacheInfo == null ? Optional.empty() : canisterCacheInfo.getActiveCanisterInterface();
        String newBody;
        try {
            RequestMetadata metadata;
            if (canisterInterface.isPresent() || urlPathInfo.requestType() == RequestType.CALL) {
                metadata = icTools.getRequestMetadata(request.body().getBytes());
            } else {
                metadata = null;
            }
            Optional<CanisterInterfaceInfo> canisterInterfaceInfo = canisterInterface.flatMap(s -> metadata.canisterMethod().map(m -> new CanisterInterfaceInfo(s, m)));


            if (urlPathInfo.requestType() == RequestType.CALL && metadata.type() == RequestType.CALL && metadata.requestId().isPresent() && callRequestSignIdentityCache.getIfPresent(metadata.requestId().get()) != null) {
                newBody = icTools.getCanisterResponseForCallRequest(metadata, canisterInterfaceInfo.map(CanisterInterfaceInfo::canisterInterface), callRequestSignIdentityCache.getIfPresent(metadata.requestId().get()));
            } else {
                newBody = icTools.decodeCanisterResponse(responseReceived.body().getBytes(), canisterInterfaceInfo);
            }
        } catch (IcToolsException e) {
            topPanel.showErrorMessage("Could not decode response with path " + request.path() +
                    "\nThis could be due to a malformed response or due to an issue with the IDL.", "IC Decoding error");
            log.logToError("Failed to decode response with path " + request.path(), e);
            newBody = String.format("Failed to decode response with path %s: %s", request.path(), e.getStackTraceAsString());
        }
        return ResponseReceivedAction.continueWith(responseReceived.withAddedHeader(IC_DECODED_HEADER_NAME, "True").withBody(newBody));
    }
}

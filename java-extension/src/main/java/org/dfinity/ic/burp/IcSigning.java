package org.dfinity.ic.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.dfinity.ic.burp.IcBurpExtension.IC_DECODED_HEADER_NAME;
import static org.dfinity.ic.burp.IcBurpExtension.IC_SIGN_IDENTITY_HEADER_NAME;

public class IcSigning implements HttpHandler {
    private final Logging log;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final InternetIdentities internetIdentities;

    public IcSigning(Logging log, IcTools icTools, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, InternetIdentities internetIdentities) {
        this.log = log;
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.internetIdentities = internetIdentities;
    }

    /**
     * TODO Atm, in case of failure we simply return null which means no request is sent. Ideally, the user gets a clear error message. Pop-up?
     *
     * @param requestToBeSent information about the HTTP request that is going to be sent.
     *
     * @return
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if(!requestToBeSent.toolSource().isFromTool(ToolType.REPEATER) && !requestToBeSent.toolSource().isFromTool(ToolType.INTRUDER)) {
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        log.logToOutput("IcSigning.handleHttpRequestToBeSent - From Proxy with x-ic-decoded: " + requestToBeSent.header("x-ic-decoded").value());
        if(!requestToBeSent.header(IC_DECODED_HEADER_NAME).value().equals("True")) return RequestToBeSentAction.continueWith(requestToBeSent);

        Optional<String> cid = IcHttpRequestResponseViewer.getCanisterId(requestToBeSent.path());
        Optional<String> idlopt = Optional.empty();
        if (cid.isPresent()) {
            CompletableFuture<CanisterCacheInfo> info = this.canisterInterfaceCache.getIfPresent(cid.get());
            if (info != null)
                idlopt = info.join().getActiveCanisterInterface();
        }

        try {
            String anchor = requestToBeSent.header(IC_SIGN_IDENTITY_HEADER_NAME).value();
            if(anchor.isBlank()){
                // Either the original request wasn't signed with an II or the II was not onboarded to the plugin yet.
                // Retry to find the II in case the user removed the header.
                // TODO Remove
                // anchor = internetIdentities.findAnchor(requestToBeSent, requestToBeSent.headerValue("Origin")).orElse(null);
                //if(anchor == null){
                //    this.log.logToError("Empty Anchor header and could not find II with same anchor as request for proxy request to " + requestToBeSent.url());
                    return null;
                //}
            }
            String origin = requestToBeSent.headerValue("Origin");
            if(origin.isBlank()) {
                this.log.logToError("Empty Origin header for proxy request to " + requestToBeSent.url());
                return null;
            }

            Optional<Identity> id = internetIdentities.findSignIdentity(anchor, origin);
            if(id.isEmpty()){
                this.log.logToError("Error retrieving signIdentity for anchor " + anchor + " and request URI " + requestToBeSent.url());
                return null;
            }

            byte[] newBody = icTools.encodeAndSignCanisterRequest(requestToBeSent.bodyToString(), idlopt, id.get());
            log.logToOutput("IcSigning.handleHttpRequestToBeSent - Sending signed and encoded request with new body: " + newBody);
            HttpRequest req = requestToBeSent.withRemovedHeader(IC_SIGN_IDENTITY_HEADER_NAME);
            req = req.withRemovedHeader(IC_DECODED_HEADER_NAME);
            req = req.withBody(ByteArray.byteArray(newBody));
            log.logToOutput("Request to be sent: \n" + req);
            return RequestToBeSentAction.continueWith(req);
        } catch (IcToolsException e) {
            this.log.logToError("Exception raised during resignin of the request: " + e);
            return null;
        }
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}

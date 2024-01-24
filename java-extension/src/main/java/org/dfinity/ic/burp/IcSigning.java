package org.dfinity.ic.burp;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.JWKIdentity;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class IcSigning implements HttpHandler {
    private final Logging log;
    private final JnaIcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final JWKIdentity defaultIdentity;
    private final InternetIdentities internetIdentities;

    public IcSigning(Logging log, JnaIcTools icTools, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, JWKIdentity defaultIdentity, InternetIdentities internetIdentities) {
        this.log = log;
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.defaultIdentity = defaultIdentity;
        this.internetIdentities = internetIdentities;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        log.logToOutput("IcSigning.handleHttpRequestToBeSent");
        if(requestToBeSent.toolSource().isFromTool(ToolType.REPEATER)){
            log.logToOutput("IcSigning.handleHttpRequestToBeSent - From Proxy");
            Optional<String> cid = IcHttpRequestResponseViewer.getCanisterId(requestToBeSent.path());
            Optional<String> idlopt = Optional.empty();
            if (cid.isPresent()) {
                CompletableFuture<CanisterCacheInfo> info = this.canisterInterfaceCache.getIfPresent(cid.get());
                if (info != null)
                    idlopt = info.join().getActiveCanisterInterface();
            }

            try {
                Optional<Identity> id = internetIdentities.findIdentity(icTools.decodeCanisterRequest(requestToBeSent.body().getBytes(), idlopt));
                if(id.isEmpty()){
                    // TODO Decide what we want to do here? Show a pop-up?
                    return null;
                }

                String newBody = new String(icTools.encodeAndSignCanisterRequest(requestToBeSent.bodyToString(), idlopt, id.get()));
                log.logToOutput("IcSigning.handleHttpRequestToBeSent - Sending signed and encoded request.");
                return RequestToBeSentAction.continueWith(requestToBeSent.withBody(newBody));
            } catch (IcToolsException e) {
                throw new RuntimeException(e);
            }
        }

        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}

package org.dfinity.ic.burp;

import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;
import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class IcCacheRefresh implements HttpHandler {
    private static final Pattern IC_API_PATH_REGEX = Pattern.compile("/api/v2/canister/(?<cid>[^/]+)/(?<rtype>query|call|read_state)");
    private final Logging log;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Cache<String, RequestMetadata> callRequestCache;
    private final Function<HttpRequest, RequestToBeSentAction> continueWithRequest;
    private final Function<HttpResponse, ResponseReceivedAction> continueWithResponse;

    public IcCacheRefresh(Logging log, IcTools icTools, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, Cache<String, RequestMetadata> callRequestCache, Optional<Function<HttpRequest, RequestToBeSentAction>> continueWithRequest, Optional<Function<HttpResponse, ResponseReceivedAction>> continueWithResponse) {
        this.log = log;
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.callRequestCache = callRequestCache;
        this.continueWithRequest = continueWithRequest.orElse(RequestToBeSentAction::continueWith);
        this.continueWithResponse = continueWithResponse.orElse(ResponseReceivedAction::continueWith);
    }

    private static Optional<UrlInfo> getUrlInfo(String path) {
        var matcher = IC_API_PATH_REGEX.matcher(path);
        if (matcher.matches() && matcher.group("cid") != null && matcher.group("rtype") != null) {
            try {
                return Optional.of(new UrlInfo(matcher.group("cid"), RequestType.valueOf(matcher.group("rtype").toUpperCase())));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        try {
            var urlInfo = getUrlInfo(requestToBeSent.path());
            if (urlInfo.isPresent()) {
                canisterInterfaceCache.get(urlInfo.get().canisterId);
                if (urlInfo.get().requestType == RequestType.CALL) {
                    var content_type = requestToBeSent.header("Content-Type");
                    var content_length = requestToBeSent.header("Content-Length");
                    if (content_type != null && content_type.value().equals("application/cbor") && content_length != null && !content_length.value().equals("0")) {
                        var metadata = icTools.getRequestMetadata(requestToBeSent.body().getBytes());
                        callRequestCache.put(metadata.requestId().orElseThrow(() -> new RuntimeException("call request does not contain request id")), metadata);
                    }
                }
            }
        } catch (IcToolsException e) {
            log.logToError("could not get metadata for " + requestToBeSent.path(), e);
        } catch (MalformedRequestException ignored) {
        }
        return continueWithRequest.apply(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return continueWithResponse.apply(responseReceived);
    }

    private static final class UrlInfo {
        public final String canisterId;
        public final RequestType requestType;

        public UrlInfo(String canisterId, RequestType requestType) {
            this.canisterId = canisterId;
            this.requestType = requestType;
        }
    }
}

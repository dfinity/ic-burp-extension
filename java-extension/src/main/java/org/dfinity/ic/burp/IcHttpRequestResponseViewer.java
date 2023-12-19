package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class IcHttpRequestResponseViewer implements ExtensionProvidedHttpRequestEditor, ExtensionProvidedHttpResponseEditor {

    private static final Pattern IC_API_PATH_REGEX = Pattern.compile("/api/v2/canister/(?<cid>[^/]+)/(query|call|read_state)");

    private final Logging log;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Cache<String, RequestMetadata> callRequestCache;
    private final RawEditor requestEditor;
    private final boolean isRequest;
    private final Function<String, ByteArray> byteArrayFactory;
    private HttpRequestResponse requestResponse;

    public IcHttpRequestResponseViewer(MontoyaApi api, IcTools icTools, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, Cache<String, RequestMetadata> callRequestCache, boolean isRequest, Optional<Function<String, ByteArray>> byteArrayFactory) {
        this.log = api.logging();
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.callRequestCache = callRequestCache;
        this.isRequest = isRequest;
        this.byteArrayFactory = byteArrayFactory.orElse(ByteArray::byteArray);
        requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
    }

    private static Optional<String> getCanisterId(String path) {
        var matcher = IC_API_PATH_REGEX.matcher(path);
        if (matcher.matches()) {
            return Optional.ofNullable(matcher.group("cid"));
        }
        return Optional.empty();
    }

    @Override
    public HttpRequest getRequest() {
        log.logToOutput("IcHttpRequestResponseViewer.getRequest");
        return requestResponse.request();
    }

    @Override
    public HttpResponse getResponse() {
        log.logToOutput("IcHttpRequestResponseViewer.getResponse");
        return requestResponse.response();
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        try {
            log.logToOutput("IcHttpRequestResponseViewer.IcHttpRequestResponseViewer");
            this.requestResponse = requestResponse;

            var cid = getCanisterId(requestResponse.request().path()).orElseThrow(() -> new RuntimeException("canister id not present in " + requestResponse.request()));
            canisterInterfaceCache.get(cid).thenAccept(canisterCacheInfo -> {
                Optional<String> canisterInterface = canisterCacheInfo == null ? Optional.empty() : Optional.of(canisterCacheInfo.getActiveCanisterInterface());
                String content;
                if (isRequest) {
                    try {
                        var res = icTools.decodeCanisterRequest(requestResponse.request().body().getBytes(), canisterInterface);
                        content = res.decodedRequest();
                    } catch (IcToolsException e) {
                        log.logToError("Failed to decode request with path " + requestResponse.request().path(), e);
                        content = String.format("Failed to decode request with path %s: %s", requestResponse.request().path(), e.getStackTraceAsString());
                    }
                } else {
                    try {
                        Optional<CanisterInterfaceInfo> canisterInterfaceInfo;
                        if (canisterInterface.isPresent()) {
                            var metadata = icTools.getRequestMetadata(requestResponse.request().body().getBytes());

                            if (requestResponse.request().path().endsWith("/query")) {
                                canisterInterfaceInfo = metadata.canisterMethod().map(m -> new CanisterInterfaceInfo(canisterInterface.get(), m));
                            } else { // read_state
                                canisterInterfaceInfo = Optional.ofNullable(callRequestCache.getIfPresent(metadata.requestId())).flatMap(RequestMetadata::canisterMethod).map(m -> new CanisterInterfaceInfo(canisterInterface.get(), m));
                            }
                        } else {
                            canisterInterfaceInfo = Optional.empty();
                        }
                        content = icTools.decodeCanisterResponse(requestResponse.response().body().getBytes(), canisterInterfaceInfo);
                    } catch (IcToolsException e) {
                        log.logToError("Failed to decode response with path " + requestResponse.request().path(), e);
                        content = String.format("Failed to decode response with path %s: %s", requestResponse.request().path(), e.getStackTraceAsString());
                    }
                }
                this.requestEditor.setContents(byteArrayFactory.apply(content));
            }).join();
        }
        catch (Exception e){
            log.logToError("Exception in setRequestResponse: " + e);
        }
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        log.logToOutput("IcHttpRequestResponseViewer.isEnabledFor");
        try {
            if (requestResponse.request() == null || (!isRequest && !requestResponse.hasResponse()))
                return false;

            try {
                var path = requestResponse.request().path();
                var content_type = isRequest ? requestResponse.request().header("Content-Type") : requestResponse.response().header("Content-Type");
                if (content_type != null && content_type.value().equals("application/cbor")) {
                    var cid = getCanisterId(path);
                    if (cid.isPresent()) {
                        // kick off interface resolution already here, so we get it faster in setRequestResponse
                        canisterInterfaceCache.get(cid.get());
                        return true;
                    }
                }
            } catch (MalformedRequestException ignored) {
            }
            return false;
        }
        catch(Exception e){
            log.logToError("Exception in isEnabledFor: " + e);
            return false;
        }
    }

    @Override
    public String caption() {
        log.logToOutput("IcHttpRequestResponseViewer.caption");
        return isRequest ? "IC Request" : "IC Response";
    }

    @Override
    public Component uiComponent() {
        log.logToOutput("IcHttpRequestResponseViewer.uiComponent");
        return requestEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        log.logToOutput("IcHttpRequestResponseViewer.selectedData");
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        log.logToOutput("IcHttpRequestResponseViewer.isModified");
        return requestEditor.isModified();
    }
}

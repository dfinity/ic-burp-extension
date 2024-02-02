package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
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
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.awt.*;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.dfinity.ic.burp.Utils.getStacktrace;

public class IcHttpRequestResponseViewer implements ExtensionProvidedHttpRequestEditor, ExtensionProvidedHttpResponseEditor {

    private static final Pattern IC_API_PATH_REGEX = Pattern.compile("/api/v2/canister/(?<cid>[^/]+)/(query|call|read_state)");

    private final Logging log;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Cache<String, RequestMetadata> callRequestCache;
    private final RawEditor requestEditor;
    private final boolean isRequest;
    private final Function<String, ByteArray> byteArrayFactory;
    private final TopPanel topPanel;
    private HttpRequestResponse requestResponse;

    public IcHttpRequestResponseViewer(MontoyaApi api, IcTools icTools, TopPanel topPanel, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, Cache<String, RequestMetadata> callRequestCache, boolean isRequest, Optional<Function<String, ByteArray>> byteArrayFactory) {
        this.log = api.logging();
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.callRequestCache = callRequestCache;
        this.isRequest = isRequest;
        this.byteArrayFactory = byteArrayFactory.orElse(ByteArray::byteArray);
        this.topPanel = topPanel;
        requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
    }

    public static Optional<String> getCanisterId(String path) {
        var matcher = IC_API_PATH_REGEX.matcher(path);
        if (matcher.matches()) {
            return Optional.ofNullable(matcher.group("cid"));
        }
        return Optional.empty();
    }

    @Override
    public HttpRequest getRequest() {
        return requestResponse.request();
    }

    @Override
    public HttpResponse getResponse() {
        return requestResponse.response();
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        try {
            this.requestResponse = requestResponse;

            var cid = getCanisterId(requestResponse.request().path()).orElseThrow(() -> new RuntimeException("Canister id not present in " + requestResponse.request()));
            canisterInterfaceCache.get(cid).thenAccept(canisterCacheInfo -> {
                Optional<String> canisterInterface = canisterCacheInfo == null ? Optional.empty() : canisterCacheInfo.getActiveCanisterInterface();
                //log.logToOutput("setRequestResponse canisterInterface used for " + cid + " is " + canisterInterface);
                String content;
                if (isRequest) {
                    try {
                        var res = icTools.decodeCanisterRequest(requestResponse.request().body().getBytes(), canisterInterface);
                        content = res.decodedRequest();
                    } catch (IcToolsException e) {
                        this.showErrorMessage("Could not decode request with path " + requestResponse.request().path() +
                                "\nThis could be due to a malformed request or due to an issue with the IDL.","IC Decoding error");
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
                                canisterInterfaceInfo = metadata.requestId().map(callRequestCache::getIfPresent).flatMap(RequestMetadata::canisterMethod).map(m -> new CanisterInterfaceInfo(canisterInterface.get(), m));
                            }
                        } else {
                            canisterInterfaceInfo = Optional.empty();
                        }
                        content = icTools.decodeCanisterResponse(requestResponse.response().body().getBytes(), canisterInterfaceInfo);
                    } catch (IcToolsException e) {
                        this.showErrorMessage("Could not decode response with path " + requestResponse.request().path() +
                                "\nThis could be due to a malformed response or due to an issue with the IDL.","IC Decoding error");
                        log.logToError("Failed to decode response with path " + requestResponse.request().path(), e);
                        content = String.format("Failed to decode response with path %s: %s", requestResponse.request().path(), e.getStackTraceAsString());
                    }
                }
                this.requestEditor.setContents(byteArrayFactory.apply(content));
            }).join();
        } catch (Exception e) {
            log.logToError("Exception in setRequestResponse: " + getStacktrace(e));
        }
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        try {
            if (requestResponse.request() == null || (!isRequest && !requestResponse.hasResponse()))
                return false;

            if(!isRequest && requestResponse.response().statusCode() != 200){
                return false;
            }

            // TODO This throws an exception from time to time. See notes for stacktrace.
            // TODO java.lang.IllegalArgumentException: fromIndex(1) > toIndex(0)

            HttpHeader icDecodedHeader = requestResponse.request().header(IcBurpExtension.IC_DECODED_HEADER_NAME);
            if (icDecodedHeader != null && icDecodedHeader.value().equals("True")) {
                return false;
            }

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
            } catch (MalformedRequestException e) {
                log.logToError("Exception in isEnabledFor: " + getStacktrace(e));
            }
            return false;
        } catch (Exception e) {
            log.logToError("Exception in isEnabledFor: " + getStacktrace(e));
            return false;
        }
    }

    @Override
    public String caption() {
        return isRequest ? "IC Request" : "IC Response";
    }

    @Override
    public Component uiComponent() {
        return requestEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return requestEditor.selection().isPresent() ? requestEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return requestEditor.isModified();
    }

    private void showErrorMessage(String message, String title){
        if(this.topPanel != null){
            topPanel.showErrorMessage(message, title);
        }
    }

}

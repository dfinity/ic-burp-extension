package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.*;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.IcTools.IcToolsException;

import java.awt.*;
import java.util.Optional;

public class IcHttpRequestResponseViewer implements ExtensionProvidedHttpRequestEditor, ExtensionProvidedHttpResponseEditor {

    private static final String IC_API_PATH_REGEX = "/api/v2/canister/(.+)/(query|call|read_state)";

    private final IcTools icTools;
    private final RawEditor requestEditor;
    private final boolean isRequest;
    private HttpRequestResponse requestResponse;

    public IcHttpRequestResponseViewer(MontoyaApi api, IcTools icTools, boolean isRequest) {
        this.icTools = icTools;
        this.isRequest = isRequest;
        requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);
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
        this.requestResponse = requestResponse;
        String content;
        if(isRequest) {
            try {
                var res = icTools.decodeCanisterRequest(requestResponse.request().body().getBytes(), Optional.empty());
                content = res.decodedRequest;
            } catch (IcToolsException e) {
                content = String.format("Failed to decode request: %s", e.getStackTraceAsString());
            }
        } else {
            try {
                content = icTools.decodeCanisterResponse(requestResponse.response().body().getBytes(), Optional.empty());
            } catch (IcToolsException e) {
                content = String.format("Failed to decode response: %s", e.getStackTraceAsString());
            }
        }
        this.requestEditor.setContents(ByteArray.byteArray(content));
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        try {
            var request_path = requestResponse.request().path();
            var content_type = isRequest ? requestResponse.request().header("Content-Type") : requestResponse.response().header("Content-Type");
            var content_length = isRequest ? requestResponse.request().header("Content-Length") : requestResponse.response().header("Content-Length");
            return content_type != null && content_type.value().equals("application/cbor") && content_length != null && !content_length.value().equals("0") && request_path.matches(IC_API_PATH_REGEX);
        } catch (MalformedRequestException e) {
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
}

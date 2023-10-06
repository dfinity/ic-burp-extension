package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.*;
import org.dfinity.ic.burp.tools.IcTools;

public class IcHttpRequestResponseViewerProvider implements HttpRequestEditorProvider, HttpResponseEditorProvider {

    private final MontoyaApi api;
    private final IcTools icTools;

    public IcHttpRequestResponseViewerProvider(MontoyaApi api, IcTools icTools) {
        this.api = api;
        this.icTools = icTools;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        return new IcHttpRequestResponseViewer(api, icTools, true);
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext creationContext) {
        return new IcHttpRequestResponseViewer(api, icTools, false);
    }
}
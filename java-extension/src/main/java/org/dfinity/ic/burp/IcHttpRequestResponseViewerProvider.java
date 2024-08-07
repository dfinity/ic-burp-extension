package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

public class IcHttpRequestResponseViewerProvider implements HttpRequestEditorProvider, HttpResponseEditorProvider {

    private final MontoyaApi api;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Cache<String, RequestMetadata> callRequestCache;
    private final TopPanel topPanel;

    public IcHttpRequestResponseViewerProvider(MontoyaApi api, IcTools icTools, TopPanel topPanel, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, Cache<String, RequestMetadata> callRequestCache) {
        this.api = api;
        this.icTools = icTools;
        this.topPanel = topPanel;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.callRequestCache = callRequestCache;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        return new IcHttpRequestResponseViewer(api, icTools, topPanel, canisterInterfaceCache, callRequestCache, true, Optional.empty());
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext creationContext) {
        return new IcHttpRequestResponseViewer(api, icTools, topPanel, canisterInterfaceCache, callRequestCache, false, Optional.empty());
    }
}

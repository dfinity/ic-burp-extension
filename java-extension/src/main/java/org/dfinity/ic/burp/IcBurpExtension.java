package org.dfinity.ic.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.UI.CacheLoaderSubscriber;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.controller.ICController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

public class IcBurpExtension implements BurpExtension {

    private AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("IC Burp Extension " + Optional.of(getClass()).map(Class::getPackage).map(Package::getImplementationVersion).orElse("DEV"));

        var icTools = new JnaIcTools();

        CacheLoaderSubscriber l = new CacheLoaderSubscriber();
        DataPersister dataPersister = new DataPersister(api.logging(), icTools, api.persistence().extensionData(), l);
        canisterInterfaceCache = dataPersister.getCanisterInterfaceCache();

        Cache<String, RequestMetadata> callRequestCache = Caffeine.newBuilder().maximumSize(10_000).build();
        var viewerProvider = new IcHttpRequestResponseViewerProvider(api, icTools, canisterInterfaceCache, callRequestCache);
        api.userInterface().registerHttpRequestEditorProvider(viewerProvider);
        api.userInterface().registerHttpResponseEditorProvider(viewerProvider);

        // Create top level UI component and have the loader delegate notifications to it to update the UI accordingly.
        ICController controller = new ICController(api.logging(), canisterInterfaceCache, dataPersister, icTools);
        TopPanel tp = new TopPanel(api.logging(), canisterInterfaceCache, controller);
        l.setDelegate(tp);
        controller.setTopPanel(tp);

        api.userInterface().registerSuiteTab("IC", tp);

        // Register an HTTP handler that intercepts all requests to update the interface cache.
        api.http().registerHttpHandler(new IcCacheRefresh(api.logging(), icTools, canisterInterfaceCache, callRequestCache, Optional.empty(), Optional.empty()));

        // Add a handler that stores the IDLs to Burp persistent storage (Burp project file) before unloading the extension.
        // This effectively stores the data before shutting down Burp if trigger normally.
        api.extension().registerUnloadingHandler(() -> dataPersister.storeCanisterInterfaceCache(canisterInterfaceCache));
    }
}

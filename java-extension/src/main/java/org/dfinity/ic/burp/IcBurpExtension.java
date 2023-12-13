package org.dfinity.ic.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.UI.CacheLoaderSubscriber;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

public class IcBurpExtension implements BurpExtension {

    private AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("IC Burp Extension " + Optional.of(getClass()).map(Class::getPackage).map(Package::getImplementationVersion).orElse("DEV"));

        var icTools = new JnaIcTools();

        CacheLoaderSubscriber l = new CacheLoaderSubscriber();
        DataPersister dataPersister = DataPersister.getInstance();
        dataPersister.init(api.logging(), icTools, api.persistence().extensionData(), l);
        canisterInterfaceCache = dataPersister.getCanisterInterfaceCache();

        Cache<String, RequestMetadata> callRequestCache = Caffeine.newBuilder().maximumSize(10_000).build();
        var viewerProvider = new IcHttpRequestResponseViewerProvider(api, icTools, canisterInterfaceCache, callRequestCache);
        api.userInterface().registerHttpRequestEditorProvider(viewerProvider);
        api.userInterface().registerHttpResponseEditorProvider(viewerProvider);

        // Create top level UI component and have the loader delegate notifications to it to update the UI accordingly.
        TopPanel tp = new TopPanel(api.logging(), canisterInterfaceCache);
        l.setDelegate(tp);

        api.userInterface().registerSuiteTab("IC", tp);

        api.http().registerHttpHandler(new IcCacheRefresh(api.logging(), icTools, canisterInterfaceCache, callRequestCache, Optional.empty(), Optional.empty()));
    }
}

package org.dfinity.ic.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.UI.CacheLoaderSubscriber;
import org.dfinity.ic.burp.UI.ContextMenu.ProxyContextMenuProvider;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.controller.IiController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.storage.DataPersister;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;


public class IcBurpExtension implements BurpExtension {

    public static final String IC_DECODED_HEADER_NAME = "x-ic-decoded";
    public static final String IC_SIGN_IDENTITY_HEADER_NAME = "x-ic-sign-identity";
    public static final String IC_FRONTEND_HOSTNAME_HEADER_NAME = "x-ic-frontend-hostname";
    private AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private InternetIdentities internetIdentities;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("IC Burp Extension " + Optional.of(getClass()).map(Class::getPackage).map(Package::getImplementationVersion).orElse("DEV"));

        IcTools icTools = new JnaIcTools();
        CacheLoaderSubscriber l = new CacheLoaderSubscriber();
        DataPersister dataPersister = new DataPersister(api.logging(), icTools, api.persistence().extensionData(), api.persistence().preferences(), l);
        canisterInterfaceCache = dataPersister.getCanisterInterfaceCache();
        this.internetIdentities = dataPersister.getInternetIdentities();

        // Create top level UI component and have the loader delegate notifications to it to update the UI accordingly.
        IdlController idlController = new IdlController(api.logging(), canisterInterfaceCache, dataPersister, icTools);
        IiController iiController = new IiController(api.logging(), internetIdentities);
        TopPanel tp = new TopPanel(api.logging(), canisterInterfaceCache, idlController, iiController, this.internetIdentities);
        l.setDelegate(tp);
        idlController.setTopPanel(tp);
        iiController.setTopPanel(tp);

        api.userInterface().registerSuiteTab("IC", tp);

        Cache<String, RequestMetadata> callRequestCache = Caffeine.newBuilder().maximumSize(10_000).build();
        var viewerProvider = new IcHttpRequestResponseViewerProvider(api, icTools, tp, canisterInterfaceCache, callRequestCache);
        api.userInterface().registerHttpRequestEditorProvider(viewerProvider);
        api.userInterface().registerHttpResponseEditorProvider(viewerProvider);

        // Register an HTTP handler that intercepts all requests to update the interface cache.
        api.http().registerHttpHandler(new IcCacheRefresh(api.logging(), icTools, internetIdentities, canisterInterfaceCache, callRequestCache, Optional.empty(), Optional.empty()));
        api.http().registerHttpHandler(new IcSigning(api.logging(), icTools, tp, canisterInterfaceCache, internetIdentities));

        // Add Context Menu item to send IC requests to the repeater.
        api.userInterface().registerContextMenuItemsProvider(new ProxyContextMenuProvider(api, icTools, canisterInterfaceCache, internetIdentities));

        // Add a handler that stores the IDLs to Burp persistent storage (Burp project file) before unloading the extension.
        // This effectively stores the data before shutting down Burp if trigger normally.
        api.extension().registerUnloadingHandler(() -> dataPersister.storeCanisterInterfaceCache(canisterInterfaceCache));
        api.extension().registerUnloadingHandler(() -> dataPersister.storeInternetIdentities(internetIdentities));
    }
}

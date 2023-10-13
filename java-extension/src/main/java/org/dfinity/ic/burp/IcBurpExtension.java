package org.dfinity.ic.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

public class IcBurpExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("IC Burp Extension " + Optional.of(getClass()).map(Class::getPackage).map(Package::getImplementationVersion).orElse("DEV"));

        var icTools = new JnaIcTools();
        AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache = Caffeine.newBuilder().maximumSize(10_000).buildAsync(
                cid -> {
                    try {
                        return new CanisterCacheInfo(icTools.discoverCanisterInterface(cid));
                    } catch (IcToolsException e) {
                        // if there is an error we don't cache the result and try again next time
                        api.logging().logToError(String.format("discoverCanisterInterface failed for canisterId %s", cid), e);
                        return null;
                    }
                }
        );
        Cache<String, RequestMetadata> callRequestCache = Caffeine.newBuilder().maximumSize(10_000).build();
        var viewerProvider = new IcHttpRequestResponseViewerProvider(api, icTools, canisterInterfaceCache, callRequestCache);
        api.userInterface().registerHttpRequestEditorProvider(viewerProvider);
        api.userInterface().registerHttpResponseEditorProvider(viewerProvider);

        api.http().registerHttpHandler(new IcCacheRefresh(api.logging(), icTools, canisterInterfaceCache, callRequestCache, Optional.empty(), Optional.empty()));
    }
}

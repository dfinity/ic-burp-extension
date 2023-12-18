package org.dfinity.ic.burp;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.PersistedObject;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.UI.CacheLoaderSubscriber;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.Map;
import java.util.Optional;

public class DataPersister {
    private PersistedObject rootPO;
    private JnaIcTools icTools;
    private Logging log;
    private CacheLoaderSubscriber cacheLoaderSubscriber;


    public DataPersister(Logging log, JnaIcTools icTools, PersistedObject rootPO, CacheLoaderSubscriber cacheLoaderSubscriber) {
        this.rootPO = rootPO;
        this.log = log;
        this.icTools = icTools;
        this.cacheLoaderSubscriber = cacheLoaderSubscriber;
    }

    public AsyncLoadingCache<String, CanisterCacheInfo> getCanisterInterfaceCache(){

        AsyncLoadingCache<String, CanisterCacheInfo> cache = Caffeine.newBuilder().buildAsync(
                    cid -> {
                        if(cid == null) {
                            log.logToError("Argument provided to the cache is not a String.");
                            return null;
                        }
                        log.logToOutput("Resolving canister interface for: " + cid);
                        CanisterCacheInfo val;
                        try {
                            Optional<String> idl = icTools.discoverCanisterInterface(cid);
                            InterfaceType t = idl.isPresent() ? InterfaceType.AUTOMATIC : InterfaceType.FAILED;
                            val = new CanisterCacheInfo(idl, t);
                        } catch (IcToolsException e) {
                            log.logToError(String.format("discoverCanisterInterface failed for canisterId %s", cid), e);
                            val = new CanisterCacheInfo(Optional.empty(), InterfaceType.FAILED);
                        }
                        cacheLoaderSubscriber.onCacheLoad();
                        return val;
                    }
            );
        log.logToOutput("rootPO child object keys: " + rootPO.childObjectKeys());
        PersistedObject icObject = rootPO.getChildObject("IC");
        if(icObject == null) return cache;
        log.logToOutput("icObject child object keys: " + icObject.childObjectKeys());
        PersistedObject canisterInterfaceCachePO =  icObject.getChildObject("CanisterInterfaceCache");
        if(canisterInterfaceCachePO == null) return cache;

        log.logToOutput("Adding the following CIDs to cache from persistent storage: \n" + canisterInterfaceCachePO.childObjectKeys());

        for (String cid : canisterInterfaceCachePO.childObjectKeys()){
            PersistedObject canisterCacheInfoPO = canisterInterfaceCachePO.getChildObject(cid);
            if(canisterCacheInfoPO == null) continue; // This shouldn't happen as we just fetched the available keys.
            CanisterCacheInfo info = new CanisterCacheInfo();

            log.logToOutput("ActiveCanisterInterfaceType from storage: " + canisterCacheInfoPO.getString("ActiveCanisterInterfaceType"));
            try {
                InterfaceType type = InterfaceType.valueOf(canisterCacheInfoPO.getString("ActiveCanisterInterfaceType"));
                info.setActiveCanisterInterfaceType(type);
            } catch (Exception e){
                log.logToError("Active canister interface type could not be deserialized to enum.");
            }
            log.logToOutput("nextPO keys: " + canisterCacheInfoPO.childObjectKeys());
            for(String type: canisterCacheInfoPO.childObjectKeys()){
                PersistedObject canisterInterfacePO = canisterCacheInfoPO.getChildObject(type);
                if(canisterInterfacePO == null) continue; // This shouldn't happen as we just fetched the available keys.
                String idl = canisterInterfacePO.getString("IDL");
                Optional<String> idlOpt = idl == null || idl.isEmpty() ? Optional.empty() : Optional.of(idl);
                try {
                    info.putCanisterInterface(idlOpt, InterfaceType.valueOf(type));
                } catch (Exception e){
                    log.logToError("Canister interface type could not be deserialized to enum. The IDL was not restored properly for canister: " + cid);
                }
            }
            cache.synchronous().put(cid, info);
        }
        return cache;
    }

    public boolean storeCanisterInterfaceCache(AsyncLoadingCache<String, CanisterCacheInfo> cache){
        /* The strategy is to create a new PersistedObject.
            Overall, the data structure looks like a tree:

         BurpRoot (PersistedObject)
                    |
                    |
         ICObject (PersistedObject)
                    |
                    |
        CanisterInterfaceCache (PersistedObject)
                    |
                    |   Key = CID
                    |
            CanisterCacheInfo (PersistedObject): Each such persisted object will have the activeCanisterInterfaceType serialized to a String.
                    |                            The key for activeCanisterInterfaceType is "ActiveCanisterInterfaceType".
                    |   Key = InterfaceType.toString()
                    |
            CanisterInterface (PersistedObject)
                    |
                    |   Key = "IDL"
                    |
                   IDL (String): This is the IDL content.
        */

        log.logToOutput("Storing IDLs to Burp project file.");
        PersistedObject canisterInterfaceCachePO = generatePOTree(rootPO);

        for(Map.Entry<String, CanisterCacheInfo> cacheEntry : cache.synchronous().asMap().entrySet()){
            PersistedObject canisterCacheInfoPO = PersistedObject.persistedObject();
            CanisterCacheInfo info = cacheEntry.getValue();
            canisterCacheInfoPO.setString("ActiveCanisterInterfaceType", info.getActiveCanisterInterfaceType().name());

            for(Map.Entry<InterfaceType, Optional<String>> canisterInterface : info.getCanisterInterfaces().entrySet()){
                PersistedObject canisterInterfacePO = PersistedObject.persistedObject();
                canisterInterfacePO.setString("IDL", canisterInterface.getValue().orElse(""));
                canisterCacheInfoPO.setChildObject(canisterInterface.getKey().name(), canisterInterfacePO);
            }
            canisterInterfaceCachePO.setChildObject(cacheEntry.getKey(), canisterCacheInfoPO);
        }
        return true;
    }

    private PersistedObject generatePOTree(PersistedObject root){
        PersistedObject icPO = PersistedObject.persistedObject();

        PersistedObject canisterInterfaceCachePO = PersistedObject.persistedObject();
        icPO.setChildObject("CanisterInterfaceCache", canisterInterfaceCachePO);

        this.rootPO.setChildObject("IC", icPO);

        return canisterInterfaceCachePO;
    }
}

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
    private static final DataPersister instance = new DataPersister();


    public static DataPersister getInstance(){
        return instance;
    }

    public void init(Logging log, JnaIcTools icTools, PersistedObject rootPO, CacheLoaderSubscriber cacheLoaderSubscriber) {
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
            PersistedObject nextPO = canisterInterfaceCachePO.getChildObject(cid);
            if(nextPO == null) continue; // This shouldn't happen as we just fetched the available keys.
            CanisterCacheInfo info = new CanisterCacheInfo();

            log.logToOutput("ActiveCanisterInterfaceType from storage: " + nextPO.getString("ActiveCanisterInterfaceType"));
            try {
                InterfaceType type = InterfaceType.valueOf(nextPO.getString("ActiveCanisterInterfaceType"));
                info.setActiveCanisterInterfaceType(type);
            } catch (Exception e){
                log.logToError("Active canister interface type could not be deserialized to enum.");
            }

            for(String t: nextPO.stringKeys()){
                if(t.equals("ActiveCanisterInterfaceType")) continue;
                log.logToOutput("nextPO keys: " + t);
                String idl = nextPO.getString(t);
                if(idl == null) continue; // This shouldn't happen as we just fetched the available keys.
                Optional<String> idlOpt = idl.isEmpty() ? Optional.empty() : Optional.of(idl);
                try {
                    info.putCanisterInterface(idlOpt, InterfaceType.valueOf(t));
                } catch (Exception e){
                    log.logToError("Canister interface type could not be deserialized to enum. The IDL was not restored properly for canister: " + cid);
                }
            }
            cache.synchronous().put(cid, info);

        }

        return cache;
    }

    public void storeCanisterInterfaceCache(AsyncLoadingCache<String, CanisterCacheInfo> cache){
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
                    |
                    |   Key = InterfaceType.toString()
                    |
            CanisterInterface (String): This is a String per InterfaceType where the key is the serialized InterfaceType and the value is the IDL content.
                                        Possibly this also becomes a PersistedObject in the future if the CanisterInterface gets more attributes.
        */

        log.logToOutput("Storing IDLs to Burp project file.");
        PersistedObject canisterInterfaceCachePO = generatePOTree(rootPO);

        for(Map.Entry<String, CanisterCacheInfo> entry : cache.synchronous().asMap().entrySet()){
            PersistedObject nextPO = PersistedObject.persistedObject();
            CanisterCacheInfo info = entry.getValue();
            nextPO.setString("ActiveCanisterInterfaceType", info.getActiveCanisterInterfaceType().name());

            for(Map.Entry<InterfaceType, Optional<String>> i : info.getCanisterInterfaces().entrySet()){
                nextPO.setString(i.getKey().name(), i.getValue().orElse(""));
            }
            canisterInterfaceCachePO.setChildObject(entry.getKey(), nextPO);
        }
    }

    private PersistedObject generatePOTree(PersistedObject root){
        PersistedObject icPO = PersistedObject.persistedObject();

        PersistedObject canisterInterfaceCachePO = PersistedObject.persistedObject();
        icPO.setChildObject("CanisterInterfaceCache", canisterInterfaceCachePO);

        this.rootPO.setChildObject("IC", icPO);

        return canisterInterfaceCachePO;
    }
}

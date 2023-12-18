package org.dfinity.ic.burp;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.Map;
import java.util.Optional;

public class ICController {
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final DataPersister dataPersister;
    private final Logging log;
    private final JnaIcTools icTools;
    private Optional<String> selectedCID;
    private Optional<InterfaceType> selectedType;

    public ICController(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, DataPersister dataPersister, JnaIcTools icTools) {
        this.log = log;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.dataPersister = dataPersister;
        this.icTools = icTools;
    }

    public boolean storeCanisterInterfaceCache() {
        return dataPersister.storeCanisterInterfaceCache(canisterInterfaceCache);
    }

    public boolean refreshAllInterfaceCacheEntries() {
        for(Map.Entry<String, CanisterCacheInfo> entry : canisterInterfaceCache.synchronous().asMap().entrySet()){
            String cid = entry.getKey();
            try {
                Optional<String> idl = icTools.discoverCanisterInterface(cid);
                CanisterCacheInfo info = entry.getValue();
                info.putCanisterInterface(idl, InterfaceType.AUTOMATIC);
            } catch (IcToolsException e) {
                log.logToError("Could not refresh IDLs with exception: + \n" + e);
                return false;
            }
        };
        return true;
    }

    public boolean refreshInterfaceCacheEntries(String cid) {
        try {
            Optional<String> idl = icTools.discoverCanisterInterface(cid);
            CanisterCacheInfo info = canisterInterfaceCache.get(cid).join();
            if(info == null) {
                return false;
            }
            info.putCanisterInterface(idl, InterfaceType.AUTOMATIC);
        } catch (IcToolsException e) {
            log.logToError("Could not refresh IDLs with exception: + \n" + e);
            return false;
        }
        return true;
    }

    public boolean updateSelectedIDL(String idl){
        if(selectedCID.isEmpty() || selectedType.isEmpty()){
            return false;
        }
        CanisterCacheInfo info = canisterInterfaceCache.get(selectedCID.get()).join();
        info.putCanisterInterface(Optional.of(idl), selectedType.get());
        return true;
    }

    public void setSelectedCID(Optional<String> cid) {
        this.selectedCID = cid;
        // TODO trigger update of the IDL text area and the IDL Table.
        // Basically move reloadIdlFromSelection and reloadIDLTable here.
    }

    public void setSelectedType(Optional<InterfaceType> type){
        this.selectedType = type;
    }
}

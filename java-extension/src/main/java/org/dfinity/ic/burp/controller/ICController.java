package org.dfinity.ic.burp.controller;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.DataPersister;
import org.dfinity.ic.burp.UI.TopPanel;
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
    private TopPanel topPanel;
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

    public boolean clearCanisterInterfaceCache() {
        return dataPersister.clearCanisterInterfaceCache();
    }

    public boolean refreshAllInterfaceCacheEntries() {
        for(Map.Entry<String, CanisterCacheInfo> entry : canisterInterfaceCache.synchronous().asMap().entrySet()){
            String cid = entry.getKey();
            try {
                Optional<String> idlOpt = icTools.discoverCanisterInterface(cid);
                if(idlOpt.isEmpty())
                    continue;

                CanisterCacheInfo info = entry.getValue();
                info.putCanisterInterface(idlOpt.get(), InterfaceType.AUTOMATIC);
            } catch (IcToolsException e) {
                log.logToError("Could not refresh IDLs.", e);
                return false;
            }
        };
        return true;
    }

    public boolean refreshInterfaceCacheEntries(String cid) {
        try {
            Optional<String> idlOpt = icTools.discoverCanisterInterface(cid);
            CanisterCacheInfo info = canisterInterfaceCache.get(cid).join();
            if(info == null) {
                return false;
            }
            if(idlOpt.isEmpty())
                return false;

            info.putCanisterInterface(idlOpt.get(), InterfaceType.AUTOMATIC);
        } catch (IcToolsException e) {
            log.logToError("Could not refresh IDLs", e);
            return false;
        }
        return true;
    }

    public boolean updateSelectedIDL(String idl){
        log.logToOutput("Updating IDL for CID: " + selectedCID + " and type: " + selectedType);
        if(selectedCID.isEmpty() || selectedType.isEmpty()){
            return false;
        }
        CanisterCacheInfo info = canisterInterfaceCache.get(selectedCID.get()).join();
        info.putCanisterInterface(idl, selectedType.get());
        return true;
    }

    public void setSelectedCID(Optional<String> cid) {
        log.logToOutput("ICController.setSelectedCID");

        this.selectedCID = cid;

        if (cid.isEmpty()) {
            return;
        } else {
            log.logToOutput("Set IDL Panel to visible");
            topPanel.showIdlPanel();
        }

        // Since a change in CID also unsets the type selection in the UI, we set this value to empty.
        setSelectedType(Optional.empty());
        topPanel.reloadIDLTable();
    }

    public void setSelectedType(Optional<InterfaceType> type){
        log.logToOutput("ICController.setSelectedType");
        this.selectedType = type;
        reloadIDLContent();
    }

    public void reloadIDLContent(){
        log.logToOutput("ICController.reloadIDLContent");

        if(selectedCID.isEmpty() || selectedType.isEmpty()){
            this.topPanel.setIDLContent("Select a canister and interface type.");
            return;
        }

        CanisterCacheInfo info = canisterInterfaceCache.get(selectedCID.get()).join();
        String idl = info.getCanisterInterface(selectedType.get());

        this.topPanel.setIDLContent(idl);
    }

    public void setTopPanel(TopPanel tp) {
        this.topPanel = tp;
    }
}

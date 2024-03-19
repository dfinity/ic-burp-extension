package org.dfinity.ic.burp.controller;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.JWKIdentity;
import org.dfinity.ic.burp.storage.DataPersister;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IdlController {
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final DataPersister dataPersister;
    private final Logging log;
    private final IcTools icTools;
    private TopPanel topPanel;
    private Optional<String> selectedCID;
    private Optional<InterfaceType> selectedType;

    public IdlController(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, DataPersister dataPersister, IcTools icTools) {
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

    public List<String> refreshAllInterfaceCacheEntries() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, CanisterCacheInfo> entry : canisterInterfaceCache.synchronous().asMap().entrySet()) {
            String cid = entry.getKey();
            if (!this.refreshInterfaceCacheEntries(cid))
                result.add(cid);
        }
        return result;
    }

    public boolean refreshInterfaceCacheEntries(String cid) {
        try {
            Optional<String> idlOpt = icTools.discoverCanisterInterface(cid);
            CanisterCacheInfo info = canisterInterfaceCache.get(cid).join();
            if (info == null) {
                return false;
            }
            if (idlOpt.isEmpty())
                return false;

            info.putCanisterInterface(idlOpt.get(), InterfaceType.AUTOMATIC);
        } catch (IcToolsException e) {
            log.logToError("Could not refresh IDL", e);
            return false;
        }
        return true;
    }

    public void updateSelectedIDL(String idl) {
        log.logToOutput("Updating IDL for CID: " + selectedCID + " and type: " + selectedType);
        if (selectedCID.isEmpty() || selectedType.isEmpty()) {
            return;
        }
        CanisterCacheInfo info = canisterInterfaceCache.get(selectedCID.get()).join();
        info.putCanisterInterface(idl, selectedType.get());
    }

    public void setSelectedCID(Optional<String> cid) {
        this.selectedCID = cid;

        if (cid.isEmpty()) {
            return;
        } else {
            topPanel.showIdlPanel();
        }

        // Since a change in CID also unsets the type selection in the UI, we set this value to empty.
        setSelectedType(Optional.empty());
        topPanel.reloadIDLTable();
    }

    public void setSelectedType(Optional<InterfaceType> type) {
        this.selectedType = type;
        reloadIDLContent();
    }

    public void reloadIDLContent() {
        if (selectedCID.isEmpty() || selectedType.isEmpty()) {
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

    public JWKIdentity getDefaultIdentity() {
        return dataPersister.getDefaultIdentity();
    }

    public void addCanister() {
        String cid = this.topPanel.getUserInput("New canister ID: ", "aaaaa-aaaaa-aaaaa-aaaaa-aaa");
        if (cid != null && !cid.isBlank() && !cid.matches("\\w{5}-\\w{5}-\\w{5}-\\w{5}-\\w{3}")) {
            this.topPanel.showErrorMessage("Wrong format! \n\nA canister ID looks as follows:\naaaaa-aaaaa-aaaaa-aaaaa-aaa",
                    "Format error");
            return;
        }
        if (this.canisterInterfaceCache.getIfPresent(cid) != null) {
            this.topPanel.showInfoMessage("Canister already exists", "Canister already exists");
            return;
        }
        this.canisterInterfaceCache.get(cid);

        this.topPanel.onCacheLoad();
        this.topPanel.reloadIdlFromSelection();
        this.topPanel.reloadIDLTable();
    }
}

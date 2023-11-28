package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.*;
import java.util.Optional;

public class IDLManagementPanel extends JSplitPane {
    private final IDLPanel idlPanel;
    private final CanisterIdPanel canisterIDPanel;
    private final Logging log;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    public IDLManagementPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache) {
        super(JSplitPane.HORIZONTAL_SPLIT);

        this.log = log;
        this.canisterInterfaceCache = canisterInterfaceCache;

        this.canisterIDPanel = new CanisterIdPanel(log, canisterInterfaceCache, this);
        this.idlPanel = new IDLPanel(log, canisterInterfaceCache, this);

        this.add(this.canisterIDPanel);
        this.add(this.idlPanel);

        this.setDividerSize(5);
        this.setDividerLocation(250);
        this.idlPanel.setVisible(false);
    }

    public synchronized void onCacheLoad() {
        log.logToOutput("IDLManagementPanel.onCacheLoad");
        this.canisterIDPanel.onCacheLoad();
    }

    public void reloadIdlFromSelection(){
        Optional<String> val = canisterIDPanel.getSelectedCID();
        if (val.isEmpty()) {
            return;
        } else {
            log.logToOutput("Set IDL Panel to visible");
            idlPanel.setVisible(true);
            this.setDividerLocation(250);
        }

        Optional<InterfaceType> t = idlPanel.getSelectedType();
        log.logToOutput("Type selected in IDL table: " + t);
        String idl;

        idl = t.map(interfaceType -> canisterInterfaceCache.synchronous().get(val.get()).getCanisterInterface(interfaceType).orElse("NOT FOUND")).orElse("NOT FOUND");

        idlPanel.setIDLContent(idl);
    }

    public Optional<String> getSelectedCID(){
        return canisterIDPanel.getSelectedCID();
    }

    public Optional<InterfaceType> getSelectedType(){
        return idlPanel.getSelectedType();
    }

    public void reloadIDLTable(){
        log.logToOutput("IDLManagementPanel.reloadIDLTable");
        idlPanel.reloadIDLTable();
    }
}

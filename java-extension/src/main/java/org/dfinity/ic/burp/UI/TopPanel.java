package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.IDL.IDLManagementPanel;
import org.dfinity.ic.burp.UI.IdentityInjection.IdentityInjectionPanel;
import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;

import javax.swing.*;

public class TopPanel extends JTabbedPane {
    private final IDLManagementPanel idlManagementPanel;
    private final IdentityInjectionPanel identityInjectionPanel;
    private final Logging log;
    private final IdlController idlController;
    private final InternetIdentityPanel internetIdentityPanel;

    public TopPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache, IdlController idlController, InternetIdentities internetIdentities) {
        this.idlController = idlController;
        this.idlManagementPanel = new IDLManagementPanel(log, idlController, canisterInterfaceCache);
        this.identityInjectionPanel = new IdentityInjectionPanel(log, idlController);
        this.internetIdentityPanel = new InternetIdentityPanel(log, internetIdentities);
        this.log = log;
        this.add("IDL Management", idlManagementPanel);
        this.add("Identity Injection", identityInjectionPanel);
        this.add("Internet Identity", internetIdentityPanel);

    }

    /**
     * Gets called whenever a new canister is added to the cache. This requires a refresh of the UI.
    */
    public void onCacheLoad() {
        idlManagementPanel.onCacheLoad();
    }

    public void reloadIdlFromSelection() {
        idlManagementPanel.reloadIdlFromSelection();
    }

    public void setIDLContent(String idl){
        idlManagementPanel.setIDLContent(idl);
    }

    public void showIdlPanel() {
        idlManagementPanel.showIdlPanel();
    }

    public void reloadIDLTable() {
        idlManagementPanel.reloadIDLTable();
    }
}

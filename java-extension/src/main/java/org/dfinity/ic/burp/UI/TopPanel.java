package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.IDL.IDLManagementPanel;
import org.dfinity.ic.burp.UI.IdentityInjection.IdentityInjectionPanel;
import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.controller.ICController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;

import javax.swing.*;

public class TopPanel extends JTabbedPane {
    private final IDLManagementPanel idlManagementPanel;
    private final IdentityInjectionPanel identityInjectionPanel;
    private final Logging log;
    private final ICController controller;
    private final InternetIdentityPanel internetIdentityPanel;

    public TopPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache, ICController controller, InternetIdentities internetIdentities) {
        this.controller = controller;
        this.idlManagementPanel = new IDLManagementPanel(log, controller, canisterInterfaceCache);
        this.identityInjectionPanel = new IdentityInjectionPanel(log, controller);
        this.internetIdentityPanel = new InternetIdentityPanel(log, controller, internetIdentities);
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

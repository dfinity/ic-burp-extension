package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.IDL.IDLManagementPanel;
import org.dfinity.ic.burp.UI.IdentityInjection.IdentityInjectionPanel;
import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.controller.IiController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.OkOrCancel;

import javax.swing.*;

public class TopPanel extends JTabbedPane {
    private final IDLManagementPanel idlManagementPanel;
    private final IdentityInjectionPanel identityInjectionPanel;
    private final Logging log;
    private final IdlController idlController;
    private final InternetIdentityPanel internetIdentityPanel;

    public TopPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache, IdlController idlController, IiController iiController, InternetIdentities internetIdentities) {
        this.idlController = idlController;
        this.idlManagementPanel = new IDLManagementPanel(log, idlController, canisterInterfaceCache);
        this.identityInjectionPanel = new IdentityInjectionPanel(log, idlController);
        this.internetIdentityPanel = new InternetIdentityPanel(log, iiController, internetIdentities);
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


    public void setIDLContent(String idl){
        idlManagementPanel.setIDLContent(idl);
    }

    public void showIdlPanel() {
        idlManagementPanel.showIdlPanel();
    }

    public void reloadIDLTable() {
        idlManagementPanel.reloadIDLTable();
    }

    public void showErrorMessage(String message, String title){
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showInfoMessage(String message, String title){
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public OkOrCancel showOkOrCancelMessage(String message, String title) {
        switch (JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION)) {
            case JOptionPane.CANCEL_OPTION:
                return OkOrCancel.CANCEL;
            default:
                return OkOrCancel.OK;
        }
    }
}

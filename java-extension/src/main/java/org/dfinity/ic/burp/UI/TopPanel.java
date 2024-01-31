package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.IDL.IDLManagementPanel;
import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.controller.IiController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.OkOrCancel;

import javax.swing.*;

public class TopPanel extends JTabbedPane {
    private final IDLManagementPanel idlManagementPanel;
    //private final IdentityInjectionPanel identityInjectionPanel;
    private final Logging log;
    private final InternetIdentityPanel internetIdentityPanel;

    public TopPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache, IdlController idlController, IiController iiController, InternetIdentities internetIdentities) {
        this.log = log;

        this.idlManagementPanel = new IDLManagementPanel(log, idlController, canisterInterfaceCache);
        this.add("IDL Management", idlManagementPanel);

        // Currently this key is never used and this complete feature can only be implemented once agent-js and agent-rs used the same curve.
        // this.identityInjectionPanel = new IdentityInjectionPanel(log, idlController);
        // this.add("Identity Injection", identityInjectionPanel);

        this.internetIdentityPanel = new InternetIdentityPanel(log, iiController, internetIdentities);
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
        if (JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
            return OkOrCancel.CANCEL;
        }
        return OkOrCancel.OK;
    }

    public String getUserInput(String message, String initialValue) {
        return JOptionPane.showInputDialog(this, message, initialValue);
    }

    public void reloadIdlFromSelection() {
        this.idlManagementPanel.reloadIdlFromSelection();
    }
}

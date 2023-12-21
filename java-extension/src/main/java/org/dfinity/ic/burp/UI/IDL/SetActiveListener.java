package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class SetActiveListener implements ActionListener {

    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    public SetActiveListener(Logging log, IDLManagementPanel idlManagementPanel, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.canisterInterfaceCache = canisterInterfaceCache;
    }

    public void actionPerformed(ActionEvent evt) {
        log.logToOutput("SetActiveListener.actionPerformed: " + evt);

        try {
            Optional<String> cid = idlManagementPanel.getSelectedCID();
            if(cid.isEmpty()){
                JOptionPane.showMessageDialog(idlManagementPanel, "Please select a canister ID from the list.",
                        "No CID Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Optional<InterfaceType> type = idlManagementPanel.getSelectedType();
            if(type.isEmpty()){
                JOptionPane.showMessageDialog(idlManagementPanel, "Please select an IDL from the list.",
                        "No IDL Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                CompletableFuture<CanisterCacheInfo> idl = canisterInterfaceCache.getIfPresent(cid.get());
                if(idl == null){
                    JOptionPane.showMessageDialog(idlManagementPanel, "Error setting IDL as active.",
                            "Activation failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                idl.get().setActiveCanisterInterfaceType(type.get());
            }
            catch (Exception e){
                JOptionPane.showMessageDialog(idlManagementPanel, "Failed to set the IDL as active.",
                        "Activation failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            idlManagementPanel.reloadIDLTable();
        }
        catch (Exception e){
           log.logToError("Error occurred activating IDL: " + e);
        }
    }
}
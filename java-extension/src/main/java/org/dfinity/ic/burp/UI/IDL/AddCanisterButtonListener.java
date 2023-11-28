package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddCanisterButtonListener implements ActionListener {

    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    public AddCanisterButtonListener(Logging log, IDLManagementPanel idlManagementPanel, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.canisterInterfaceCache = canisterInterfaceCache;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        log.logToOutput("AddCanisterButtonListener.actionPerformed: " + evt);
        String cid = JOptionPane.showInputDialog(idlManagementPanel, "New canister ID: ", "aaaaa-aaaaa-aaaaa-aaaaa-aaa");
        if(cid != null && !cid.isBlank() && !cid.matches("\\w{5}-\\w{5}-\\w{5}-\\w{5}-\\w{3}")){
            JOptionPane.showMessageDialog(idlManagementPanel, "Wrong format! \n\nA canister ID looks as follows:\naaaaa-aaaaa-aaaaa-aaaaa-aaa",
                    "Format error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.canisterInterfaceCache.get(cid);
        idlManagementPanel.onCacheLoad();
        idlManagementPanel.reloadIdlFromSelection();
        idlManagementPanel.reloadIDLTable();
    }
}

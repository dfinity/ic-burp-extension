package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.controller.ICController;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Optional;

public class IDLSelectionListener  implements ListSelectionListener {
    private final IDLManagementPanel idlManagementPanel;
    private final Logging log;
    private final ICController icController;

    public IDLSelectionListener(Logging log, IDLManagementPanel idlManagementPanel, ICController controller) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.icController = controller;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting() || !(e.getSource() instanceof DefaultListSelectionModel)) {
            icController.setSelectedType(Optional.empty());
            return;
        }

        log.logToOutput("IDLSelectionListener.valueChanged: " + e);
        icController.setSelectedType(idlManagementPanel.getSelectedType());
        //idlManagementPanel.reloadIdlFromSelection();
    }
}

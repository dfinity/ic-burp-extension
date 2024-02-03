package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.controller.IdlController;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Optional;

public class CIDSelectionListener implements ListSelectionListener {
    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;
    private final IdlController icController;

    public CIDSelectionListener(Logging log, IDLManagementPanel idlManagementPanel, IdlController icController) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.icController = icController;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting() || !(e.getSource() instanceof DefaultListSelectionModel)) {
            icController.setSelectedCID(Optional.empty());
            return;
        }
        icController.setSelectedCID(idlManagementPanel.getSelectedCID());
    }
}

package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.ICController;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class CIDSelectionListener implements ListSelectionListener {
    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;
    private final ICController icController;

    public CIDSelectionListener(Logging log, IDLManagementPanel idlManagementPanel, ICController icController) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.icController = icController;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting() || !(e.getSource() instanceof DefaultListSelectionModel)) return;

        //DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) e.getSource();
        //icController.setSelectedCID(selectionModel.getAnchorSelectionIndex());

        icController.setSelectedCID(idlManagementPanel.getSelectedCID());

        idlManagementPanel.reloadIdlFromSelection();
        idlManagementPanel.reloadIDLTable();
    }
}

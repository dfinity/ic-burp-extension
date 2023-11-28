package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CIDSelectionListener implements ListSelectionListener {
    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;

    public CIDSelectionListener(Logging log, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        log.logToOutput("CIDSelectionListener.valueChanged: " + e);
        idlManagementPanel.reloadIdlFromSelection();
        idlManagementPanel.reloadIDLTable();
    }
}

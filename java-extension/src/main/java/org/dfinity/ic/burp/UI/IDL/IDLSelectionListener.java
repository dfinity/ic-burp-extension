package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class IDLSelectionListener  implements ListSelectionListener {
    private final IDLManagementPanel idlManagementPanel;
    private final Logging log;

    public IDLSelectionListener(Logging log, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        log.logToOutput("IDLSelectionListener.valueChanged: " + e);
        idlManagementPanel.reloadIdlFromSelection();
    }
}

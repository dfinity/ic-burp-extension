package org.dfinity.ic.burp.UI.InternetIdentity;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.model.InternetIdentities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class IiSelectionListener implements ListSelectionListener {
    private final InternetIdentities internetIdentities;
    private final InternetIdentityPanel internetIdentityPanel;
    private final Logging log;

    public IiSelectionListener(Logging log, InternetIdentities internetIdentities, InternetIdentityPanel internetIdentityPanel) {
        this.log = log;
        this.internetIdentities = internetIdentities;
        this.internetIdentityPanel = internetIdentityPanel;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getValueIsAdjusting() || !(e.getSource() instanceof DefaultListSelectionModel)) {
            return;
        }
        this.log.logToOutput("IiSelectionListener.valueChanged: " + e);
        this.internetIdentities.setSelectedIiAnchor(internetIdentityPanel.getSelectedIiAnchor());
    }
}

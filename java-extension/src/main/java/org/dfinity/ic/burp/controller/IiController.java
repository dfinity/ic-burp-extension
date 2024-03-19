package org.dfinity.ic.burp.controller;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.UI.TopPanel;
import org.dfinity.ic.burp.model.IiState;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.InternetIdentity;
import org.dfinity.ic.burp.model.OkOrCancel;
import org.dfinity.ic.burp.tools.model.IcToolsException;

import javax.swing.JOptionPane;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class IiController {
    private final InternetIdentities internetIdentities;
    private final Logging log;
    private TopPanel topPanel;

    public IiController(Logging log, InternetIdentities internetIdentities) {
        this.internetIdentities = internetIdentities;
        this.log = log;
    }

    public void setTopPanel(TopPanel tp) {
        this.topPanel = tp;
    }

    public void addII() {
        String anchor = JOptionPane.showInputDialog(this.topPanel, "New II Anchor: ", "123456");
        if (anchor == null) return;
        try {
            this.topPanel.showInfoMessage("Go to https://identity.ic0.app/, login with your anchor and click \"Add new passkey\". \n\nContinue once you see the page with the QR-code.", "Add passkey");
            Optional<InternetIdentity> ii = internetIdentities.addIdentity(anchor.trim());
            Optional<String> code = ii.isPresent() ? ii.get().getCode() : Optional.empty();

            if (code.isEmpty()) {
                this.topPanel.showErrorMessage("Activation code could not be obtained.", "II Creation error");
                return;
            }
            StringSelection selection = new StringSelection(code.get());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            this.topPanel.showInfoMessage("Use the following code to activate your passkey (has been copied to clipboard): " + code.get(), "II Activation");

            while (IiState.CodeObtained.equals(ii.get().getState())) {
                OkOrCancel confirmed = this.topPanel.showOkOrCancelMessage("Use the following code to activate your passkey: " + code.get(), "Onboarding II");
                if (confirmed.equals(OkOrCancel.CANCEL)) {
                    internetIdentities.remove(ii.get().getAnchor());
                    this.topPanel.showInfoMessage("Aborted", "Aborted");
                    return;
                }
            }
        } catch (IcToolsException e) {
            this.topPanel.showErrorMessage("Couldn't create II", "II Creation error");
        }
    }

    public void checkActivations() {
        boolean result;

        result = this.internetIdentities.checkActivations();

        if (!result) {
            this.topPanel.showErrorMessage("One or more II's could not be updated.", "Update error.");
        } else {
            this.topPanel.showInfoMessage("Update successfully.", "Success");
        }
    }

    public void reactivateSelected() {
        boolean result;
        try {
            result = this.internetIdentities.reactivateSelected();
        } catch (IcToolsException e) {
            result = false;
        }
        if (result) {
            this.topPanel.showInfoMessage("New activation code generated.", "Success");
        } else {
            this.topPanel.showErrorMessage("Reactivation failed. Did you select an II and is that II in \"Add new passkey\" mode?", "Failure");

        }
    }

    public void removeSelected() {
        if (this.internetIdentities.removeSelected()) {
            this.topPanel.showInfoMessage("Removal success.", "Success");
        } else {
            this.topPanel.showErrorMessage("Removal failed.", "Failure");
        }
    }

    public void getDelegation() {
        if (internetIdentities.getSelectedII() == null) {
            this.topPanel.showErrorMessage("Please select an anchor for which the delegation should be created.", "Failure");
            return;
        }
        String frontendHostname = JOptionPane.showInputDialog(this.topPanel, "Frontend Hostname: ", "https://nns.ic0.app");
        if (frontendHostname == null || frontendHostname.isBlank()) return;
        String delegation = internetIdentities.getDelegation(frontendHostname);
        if (delegation == null || delegation.isBlank()) {
            this.topPanel.showErrorMessage("Delegation issuing failed.", "Failure");
        } else {
            StringSelection selection = new StringSelection(delegation);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            this.topPanel.showInfoMessage("Delegation copied to clipboard!\nCode example for importing in rust: https://t.ly/0ZiAE", "Success");

        }
    }
}

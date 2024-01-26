package org.dfinity.ic.burp.controller;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.model.IiState;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.InternetIdentity;
import org.dfinity.ic.burp.tools.model.IcToolsException;

import javax.swing.*;
import java.util.Optional;

public class IiController {
    private final InternetIdentities internetIdentities;
    private final Logging log;
    private InternetIdentityPanel internetIdentityPanel;

    public IiController(Logging log, InternetIdentities internetIdentities, InternetIdentityPanel internetIdentityPanel) {
        this.internetIdentities = internetIdentities;
        this.internetIdentityPanel = internetIdentityPanel;
        this.log = log;
    }

    public void addII() {
        String anchor = JOptionPane.showInputDialog(this.internetIdentityPanel, "New II Anchor: ", "123456");
        if(anchor == null) return;
        try{
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Go to https://identity.ic0.app/, login with your anchor and click \"Add new passkey\". \n\nContinue once you see the page with the QR-code.", "Add passkey", JOptionPane.INFORMATION_MESSAGE);
            Optional<InternetIdentity> ii = internetIdentities.addIdentity(anchor.trim());
            Optional<String> code = ii.isPresent() ? ii.get().getCode() : Optional.empty();

            if(code.isEmpty()){
                JOptionPane.showMessageDialog(this.internetIdentityPanel, "Activation code could not be obtained.", "II Creation error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Use the following code to activate your passkey: " + code.get(), "II Activation", JOptionPane.INFORMATION_MESSAGE);

            while(IiState.CodeObtained.equals(ii.get().getState())) {
                int confirmed = JOptionPane.showConfirmDialog(this.internetIdentityPanel, "Use the following code to activate your passkey: " + code.get(), "Onboarding II", JOptionPane.OK_CANCEL_OPTION);
                if(confirmed == JOptionPane.CANCEL_OPTION){
                    internetIdentities.remove(ii.get().getAnchor());
                    JOptionPane.showMessageDialog(this.internetIdentityPanel, "Aborted", "Aborted", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (IcToolsException e){
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Couldn't create II", "II Creation error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void checkActivations(){
        boolean result;

        result = this.internetIdentities.checkActivations();

        if(!result){
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "One or more II's could not be updated.", "Update error.", JOptionPane.ERROR_MESSAGE);
        }
        else {
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Update successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void reactivateSelected() {
        boolean result;
        try {
            result = this.internetIdentities.reactivateSelected();
        } catch (IcToolsException e) {
            result = false;
        }
        if(result){
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "New activation code generated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Reactivation failed. Did you select an II and is that II in \"Add new passkey\" mode?", "Failure", JOptionPane.ERROR_MESSAGE);

        }
    }

    public void removeSelected() {
        if(this.internetIdentities.removeSelected()){
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Removal success.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        else{
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Removal failed.", "Failure", JOptionPane.ERROR_MESSAGE);
        }
    }
}

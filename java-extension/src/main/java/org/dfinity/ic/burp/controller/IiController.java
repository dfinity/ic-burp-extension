package org.dfinity.ic.burp.controller;

import org.dfinity.ic.burp.UI.InternetIdentity.InternetIdentityPanel;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.tools.model.IcToolsException;

import javax.swing.*;
import java.util.Optional;

public class IiController {
    private final InternetIdentities internetIdentities;
    private InternetIdentityPanel internetIdentityPanel;

    public IiController(InternetIdentities internetIdentities, InternetIdentityPanel internetIdentityPanel) {
        this.internetIdentities = internetIdentities;
        this.internetIdentityPanel = internetIdentityPanel;
    }

    public void addII() {
        String anchor = JOptionPane.showInputDialog(this.internetIdentityPanel, "New II Anchor: ", "123456");
        if(anchor == null) return;
        try{
            Optional<String> code = internetIdentities.addIdentity(anchor.trim());
            if(code.isEmpty()){
                JOptionPane.showMessageDialog(this.internetIdentityPanel, "Activation code could not be obtained.", "II Creation error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Use the following code to activate your passkey: " + code, "II Activation", JOptionPane.INFORMATION_MESSAGE);

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
        this.internetIdentities.reactivateSelected();
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

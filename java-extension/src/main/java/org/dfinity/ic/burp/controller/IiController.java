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
        try{
            Optional<String> code = internetIdentities.addIdentity(anchor);
            if(code.isEmpty()){
                JOptionPane.showMessageDialog(this.internetIdentityPanel, "Activation code could not be obtained. Try generating the code again later.", "II Creation error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Use the following code to activate your passkey: " + code, "II Activation", JOptionPane.INFORMATION_MESSAGE);

        } catch (IcToolsException e){
            JOptionPane.showMessageDialog(this.internetIdentityPanel, "Couldn't create II", "II Creation error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

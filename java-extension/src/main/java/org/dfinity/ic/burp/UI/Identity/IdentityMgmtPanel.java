package org.dfinity.ic.burp.UI.Identity;

import burp.api.montoya.logging.Logging;
import com.nimbusds.jose.JOSEException;
import org.dfinity.ic.burp.model.JWKIdentity;

import javax.swing.*;
import java.awt.*;

public class IdentityMgmtPanel extends JPanel {
    private final Logging log;
    private final JTextArea script;
    private final JTextArea instructions;

    public IdentityMgmtPanel(Logging log) {
        this.log = log;

        this.instructions = new JTextArea();
        instructions.setText(
                "BurpSuite automatically generated a public/private keypair which it uses to resign IC requests.\n" +
                "For every dApp that you test, you need to inject this key in the browser context\n" +
                "This will allow the browser to use the same key (and therefor identity) as BurpSuite.");
        this.instructions.setEditable(false);

        Font f1 = instructions.getFont();
        instructions.setFont(f1.deriveFont(14.0f));
        this.add(instructions);

        this.add(Box.createRigidArea(new Dimension(0,15)));

        this.script = new JTextArea();
        this.script.setPreferredSize(new Dimension(800,1000));
        this.script.setEditable(false);

        try {
            JWKIdentity id = new JWKIdentity(log);
            script.setText(id.getScript());
        } catch (JOSEException e) {
            log.logToError("Failed to generate a keypair for the default IC identity.");
        }

        this.add(script);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }

    public void setScript(String content){
        this.script.setText(content);
    }
}

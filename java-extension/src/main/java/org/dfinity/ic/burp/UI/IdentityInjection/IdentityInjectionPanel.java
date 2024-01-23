package org.dfinity.ic.burp.UI.IdentityInjection;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.model.JWKIdentity;

import javax.swing.*;
import java.awt.*;

public class IdentityInjectionPanel extends JPanel {
    private final Logging log;
    private final JTextArea script;
    private final JTextArea instructions;
    private final IdlController controller;

    public IdentityInjectionPanel(Logging log, IdlController controller) {
        this.log = log;
        this.controller = controller;

        this.instructions = new JTextArea();
        // TODO Improve instructions for the end user.
        instructions.setText(
                "BurpSuite automatically generated a public/private keypair which it uses to re-sign IC requests.\n" +
                "For every dApp that you test, you can inject this key in the browser context\n" +
                "This will allow the browser to use the same key (and therefor identity) as BurpSuite.\n" +
                "This key will take precedence if it is detected that the request was by the browser to sign. That means\n" +
                "that even if an II is present, it won't be used unless explicitly mentioned in the\n"+
                "x-ic-sign-identity header.");
        this.instructions.setEditable(false);

        Font f1 = instructions.getFont();
        instructions.setFont(f1.deriveFont(14.0f));
        this.add(instructions);
        instructions.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        instructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.gray),
                instructions.getBorder()));
        instructions.setMaximumSize(instructions.getPreferredSize());
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);


        this.add(Box.createRigidArea(new Dimension(0,15)));

        this.script = new JTextArea();
        this.script.setEditable(false);

        JWKIdentity id = controller.getDefaultIdentity();
        if(id == null)
            script.setText("Could not generate or fetch the default identity.");
        else
            script.setText(controller.getDefaultIdentity().getScript());

        script.setAlignmentX(Component.LEFT_ALIGNMENT);
        script.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        script.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.gray),
                script.getBorder()));
        script.setMaximumSize(script.getPreferredSize());

        this.add(script);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }
}

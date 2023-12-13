package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ICButton extends JButton {

    public ICButton(Logging log, String text, ActionListener listener) {
        super(text);
        this.addActionListener(listener);
        this.setBackground(Color.lightGray);
    }

    public ICButton(Logging log, String text) {
        super(text);
        this.setBackground(Color.lightGray);
    }
}

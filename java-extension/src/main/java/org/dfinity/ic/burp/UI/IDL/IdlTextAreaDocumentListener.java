package org.dfinity.ic.burp.UI.IDL;

import org.dfinity.ic.burp.controller.ICController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class IdlTextAreaDocumentListener implements DocumentListener {

    private final ICController controller;
    private final JTextArea idlTextArea;

    public IdlTextAreaDocumentListener(ICController controller, JTextArea idlTextArea){
        this.controller = controller;
        this.idlTextArea = idlTextArea;
    }
    @Override
    public void insertUpdate(DocumentEvent e) {
        this.updateIDL(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.updateIDL(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.updateIDL(e);
    }

    private void updateIDL(DocumentEvent e){
        controller.updateSelectedIDL(idlTextArea.getText());
    }
}

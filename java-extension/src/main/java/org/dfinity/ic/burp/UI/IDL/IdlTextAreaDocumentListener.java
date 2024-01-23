package org.dfinity.ic.burp.UI.IDL;

import org.dfinity.ic.burp.controller.IdlController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class IdlTextAreaDocumentListener implements DocumentListener {

    private final IdlController controller;
    private final JTextArea idlTextArea;

    public IdlTextAreaDocumentListener(IdlController controller, JTextArea idlTextArea){
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

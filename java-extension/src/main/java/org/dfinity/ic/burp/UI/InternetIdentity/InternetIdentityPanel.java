package org.dfinity.ic.burp.UI.InternetIdentity;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.controller.IiController;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Optional;

public class InternetIdentityPanel extends JPanel {
    private final Logging log;
    private final IiController iiController;
    private final JTable iiTable;

    public InternetIdentityPanel(Logging log, InternetIdentities internetIdentities) {
        this.log = log;
        this.iiController = new IiController(log, internetIdentities, this);

        iiTable = new JTable(internetIdentities);
        iiTable.getSelectionModel().addListSelectionListener(new IiSelectionListener(log, internetIdentities, this));

        JScrollPane iiTableScrollPane = new JScrollPane(iiTable);
        iiTableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a rendered which we left align and attach it to the Integer class.
        // Integers are right aligned by default. This makes them align left (same as Strings).
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.LEFT);
        iiTable.setDefaultRenderer(Integer.class, renderer);
        iiTable.setDefaultRenderer(Boolean.class, renderer);

        this.add(iiTableScrollPane);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton addIIButton = new ICButton(log, "Add", e -> {
            iiController.addII();
        });
        this.add(addIIButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton removeIIButton = new ICButton(log, "Remove", e -> {
            iiController.removeSelected();
        });
        this.add(removeIIButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton checkActivationsButton = new ICButton(log, "Refresh IIs", e -> {
            iiController.checkActivations();
        });
        this.add(checkActivationsButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton reactivateButton = new ICButton(log, "Reactive selected II", e -> {
            iiController.reactivateSelected();
        });
        this.add(reactivateButton);
        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }

    public Optional<String> getSelectedIiAnchor(){
        if(iiTable.getSelectedRow() < 0 || iiTable.getSelectedColumn() < 0){
            return Optional.empty();
        }
        Optional<Object> val = Optional.ofNullable(iiTable.getValueAt(iiTable.getSelectedRow(), 0));
        if (val.isEmpty()) {
            return Optional.empty();
        }
        // TODO, very ugly way to go from the value at (which is a String that was manipulated) to the type.
        String valString = val.get().toString();
        log.logToOutput("getSelectedIiAnchor return value: " + valString);
        return Optional.of(valString);
    }
}

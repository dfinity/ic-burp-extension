package org.dfinity.ic.burp.UI.InternetIdentity;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.controller.IiController;
import org.dfinity.ic.burp.model.InternetIdentities;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class InternetIdentityPanel extends JPanel {
    private final Logging log;
    private final IiController iiController;

    public InternetIdentityPanel(Logging log, InternetIdentities internetIdentities) {
        this.log = log;
        this.iiController = new IiController(internetIdentities, this);

        JTable iiTable = new JTable(internetIdentities);

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

        JButton removeIIButton = new ICButton(log, "Remove", e -> {
            // TODO
        });
        this.add(addIIButton);
        this.add(removeIIButton);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }
}

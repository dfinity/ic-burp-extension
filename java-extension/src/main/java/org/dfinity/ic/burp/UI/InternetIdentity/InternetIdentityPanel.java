package org.dfinity.ic.burp.UI.InternetIdentity;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.controller.ICController;
import org.dfinity.ic.burp.model.InternetIdentities;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class InternetIdentityPanel extends JPanel {
    private final Logging log;
    private final ICController controller;

    public InternetIdentityPanel(Logging log, ICController controller, InternetIdentities internetIdentities) {
        this.log = log;
        this.controller = controller;

        JTable iiTable = new JTable(internetIdentities);

        JScrollPane iiTableScrollPane = new JScrollPane(iiTable);
        iiTableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create a rendered which we left align and attach it to the Integer class.
        // Integers are right aligned by default. This makes them align left (same as Strings).
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.LEFT);
        iiTable.setDefaultRenderer(Integer.class, renderer);
        this.add(iiTableScrollPane);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }
}

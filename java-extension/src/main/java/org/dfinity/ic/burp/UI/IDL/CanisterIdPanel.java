package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.DataPersister;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public class CanisterIdPanel extends JPanel {


    private final JTable canisterIdTable;
    private final Logging log;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final IDLManagementPanel idlManagementPanel;

    public CanisterIdPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.idlManagementPanel = idlManagementPanel;

        this.add(new ICButton(log, "Store IDLs", e -> DataPersister.getInstance().storeCanisterInterfaceCache(canisterInterfaceCache)));

        canisterIdTable = new JTable(new CanisterIdTableModel(log, canisterInterfaceCache));
        canisterIdTable.setTableHeader(null);
        CIDSelectionListener cidSelectionListener = new CIDSelectionListener(log, idlManagementPanel);
        canisterIdTable.getSelectionModel().addListSelectionListener(cidSelectionListener);

        JScrollPane canisterIdTableScrollPane = new JScrollPane(canisterIdTable);
        canisterIdTableScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cidLabel = new JLabel("Canister Id");
        Font f1 = cidLabel.getFont();
        cidLabel.setFont(f1.deriveFont(f1.getStyle() | Font.BOLD));
        cidLabel.setHorizontalAlignment(SwingConstants.LEFT);

        this.add(cidLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(canisterIdTableScrollPane);
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(Box.createRigidArea(new Dimension(0, 15)));
        this.add(buildAddCanisterButton());
    }

    private JButton buildAddCanisterButton(){
        JButton add_canister_button = new JButton("Add canister");
        add_canister_button.setBackground(Color.lightGray);
        AddCanisterButtonListener addCanisterButtonListener = new AddCanisterButtonListener(log, idlManagementPanel, canisterInterfaceCache);
        add_canister_button.addActionListener(addCanisterButtonListener);

        return add_canister_button;
    }

    public Optional<String> getSelectedCID(){
        if(canisterIdTable.getSelectedRow() == -1) return Optional.empty();
        Object val = canisterIdTable.getValueAt(canisterIdTable.getSelectedRow(), canisterIdTable.getSelectedColumn());
        if (val instanceof String && !((String) val).isBlank()){
            return Optional.of(((String) val).split(" ")[0]);
        }
        return Optional.empty();
    }

    public void onCacheLoad() {
        AbstractTableModel m = (AbstractTableModel) this.canisterIdTable.getModel();
        m.fireTableDataChanged();
    }
}

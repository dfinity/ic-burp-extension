package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.DataPersister;
import org.dfinity.ic.burp.ICController;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Optional;

public class CanisterIdPanel extends JPanel {


    private final JTable canisterIdTable;
    private final Logging log;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final IDLManagementPanel idlManagementPanel;

    public CanisterIdPanel(Logging log, ICController controller, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.idlManagementPanel = idlManagementPanel;

        this.add(new ICButton(log, "Store IDLs to project file", e -> {
            if(controller.storeCanisterInterfaceCache()){
                JOptionPane.showMessageDialog(this, "Data stored successfully", "Data stored successfully", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        canisterIdTable = new JTable(new CanisterIdTableModel(log, canisterInterfaceCache));
        canisterIdTable.setTableHeader(null);
        CIDSelectionListener cidSelectionListener = new CIDSelectionListener(log, idlManagementPanel, controller);
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

        AddCanisterButtonListener addCanisterButtonListener = new AddCanisterButtonListener(log, idlManagementPanel, canisterInterfaceCache);
        this.add(new ICButton(log, "Add canister", addCanisterButtonListener));

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(new ICButton(log, "Re-fetch all IDLs", e -> {
            if(controller.refreshAllInterfaceCacheEntries()){
                JOptionPane.showMessageDialog(this, "IDLs reloaded", "IDLs reloaded", JOptionPane.INFORMATION_MESSAGE);
                idlManagementPanel.reloadIdlFromSelection();
            } else {
                JOptionPane.showMessageDialog(this, "IDLs could not be reloaded", "IDLs could not be reloaded", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(new ICButton(log, "Re-fetch selected canister IDL", e -> {
            Optional<String> cid = getSelectedCID();
            if(cid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a canister first", "Please select a canister first", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if(controller.refreshInterfaceCacheEntries(cid.get())){
                JOptionPane.showMessageDialog(this, "IDL reloaded", "IDL reloaded", JOptionPane.INFORMATION_MESSAGE);
                // Refresh IDL text area.
                idlManagementPanel.reloadIdlFromSelection();
            } else {
                JOptionPane.showMessageDialog(this, "IDL could not be reloaded", "IDL could not be reloaded", JOptionPane.INFORMATION_MESSAGE);
            }
        }));
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

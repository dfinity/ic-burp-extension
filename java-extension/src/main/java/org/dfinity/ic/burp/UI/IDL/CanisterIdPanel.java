package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.controller.IdlController;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class CanisterIdPanel extends JPanel {


    private final JTable canisterIdTable;
    private final Logging log;
    private final IdlController idlController;

    public CanisterIdPanel(Logging log, IdlController idlController, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        this.idlController = idlController;

        // This button is probably no longer required in production.
        this.add(new ICButton(log, "Store IDLs to project file", e -> {
            if(idlController.storeCanisterInterfaceCache()){
                JOptionPane.showMessageDialog(this, "Data stored successfully", "Data stored successfully", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        // This button is probably no longer required in production.
        this.add(new ICButton(log, "Clear project data", e -> {
            if(idlController.clearCanisterInterfaceCache()){
                JOptionPane.showMessageDialog(this, "Data cleared successfully", "Data cleared successfully", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        canisterIdTable = new JTable(new CanisterIdTableModel(log, canisterInterfaceCache));
        canisterIdTable.setTableHeader(null);
        CIDSelectionListener cidSelectionListener = new CIDSelectionListener(log, idlManagementPanel, idlController);
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

        this.add(new ICButton(log, "Add canister", e-> {
            idlController.addCanister();
        }));

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(new ICButton(log, "Re-fetch all IDLs", e -> {
            List<String> cids = idlController.refreshAllInterfaceCacheEntries();
            if(cids.isEmpty()){
                JOptionPane.showMessageDialog(this, "IDLs reloaded", "IC IDLs reloaded", JOptionPane.INFORMATION_MESSAGE);
                idlManagementPanel.reloadIdlFromSelection();
            } else {
                JOptionPane.showMessageDialog(this, "IDLs for the following canisters could not be reloaded:\n" + cids,
                        "IC IDL Reload Error", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        this.add(new ICButton(log, "Re-fetch selected canister IDL", e -> {
            Optional<String> cid = getSelectedCID();
            if(cid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a canister first", "Please select a canister first", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if(idlController.refreshInterfaceCacheEntries(cid.get())){
                JOptionPane.showMessageDialog(this, "IDL reloaded", "IC IDL reloaded", JOptionPane.INFORMATION_MESSAGE);
                // Refresh IDL text area.
                idlManagementPanel.reloadIdlFromSelection();
            } else {
                JOptionPane.showMessageDialog(this, "The selected IDL could not be reloaded", "IC IDL could not be reloaded", JOptionPane.INFORMATION_MESSAGE);
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
        // Refreshing the table undoes the CID selection.
        // Need to update the controller state to reflect this.
        this.idlController.setSelectedCID(Optional.empty());
    }
}

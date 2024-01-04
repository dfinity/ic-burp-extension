package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Optional;

public class IDLTableModel extends AbstractTableModel {
    private final AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache;
    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;

    public IDLTableModel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, IDLManagementPanel idlManagementPanel) {
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
    }

    @Override
    public int getRowCount() {
        int s;
        try {
            Optional<String> cid = idlManagementPanel.getSelectedCID();
            if(cid.isEmpty()){
                return 0;
            }
            s = this.canisterInterfaceCache.synchronous().get(cid.get()).getCanisterInterfaces().size();
        } catch (Exception e) {
            return 0;
        }
        return s;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }
    @Override
    public String getColumnName(int column) {
        if(column == 0){
            return "IDL";
        }
        return super.getColumnName(column);
    }
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CanisterCacheInfo c;
        try {
            Optional<String> cid = idlManagementPanel.getSelectedCID();
            if(cid.isEmpty()){
                return "Please select a canister.";
            }

            c = this.canisterInterfaceCache.synchronous().getIfPresent(cid.get());
            if(c == null)
                return "Please select a canister.";
        } catch (Exception e) {
            return "Please select a canister.";
        }
        List<InterfaceType> keys = c.getCanisterInterfaces().keySet().stream().toList();

        // Check if rowIndex is in range.
        if(rowIndex >= keys.size()) return "";

        boolean selected = c.getActiveCanisterInterfaceType() == keys.get(rowIndex);
        return keys.get(rowIndex).toString() +
                (selected ? " (ACTIVE)" : "");
    }
}

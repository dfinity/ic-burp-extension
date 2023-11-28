package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.table.AbstractTableModel;
import java.util.concurrent.ConcurrentMap;

public class CanisterIdTableModel extends AbstractTableModel {
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final Logging log;

    public CanisterIdTableModel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache) {
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.log = log;
    }

    @Override
    public int getRowCount() {
        return canisterInterfaceCache.synchronous().asMap().size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        if(column == 0){
            return "Canister Id";
        }
        return super.getColumnName(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        log.logToOutput("CanisterIdTableModel.getValueAt(" + rowIndex + ", " + columnIndex + ")");
        ConcurrentMap<String, CanisterCacheInfo> syncMap = canisterInterfaceCache.synchronous().asMap();
        Object[] keys = syncMap.keySet().toArray();

        // Check if rowIndex is in range.
        if(rowIndex >= keys.length) return "";

        String cid = (String) syncMap.keySet().toArray()[rowIndex];
        log.logToOutput("CanisterIdTableModel cid: " + cid);

        boolean isPresent;
        try {
            CanisterCacheInfo canisterCacheInfo = syncMap.get(cid);
            isPresent = canisterCacheInfo.getActiveCanisterInterface().isPresent();
        } catch (Exception e) {
            log.logToError("Exception raised while fetching the canisterCacheInfo. cid: " + cid);
            isPresent = false;
        }
        return cid + (!isPresent ? " (IDL Not Found)" : "");
    }
}

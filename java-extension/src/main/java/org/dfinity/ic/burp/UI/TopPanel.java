package org.dfinity.ic.burp.UI;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.DataPersister;
import org.dfinity.ic.burp.UI.IDL.IDLManagementPanel;
import org.dfinity.ic.burp.UI.Identity.IdentityMgmtPanel;
import org.dfinity.ic.burp.model.CanisterCacheInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TopPanel extends JTabbedPane {
    private final IDLManagementPanel idlManagementPanel;
    private final IdentityMgmtPanel identityMgmtPanel;
    private final Logging log;

    public TopPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache) {

        this.idlManagementPanel = new IDLManagementPanel(log, canisterInterfaceCache);
        this.identityMgmtPanel = new IdentityMgmtPanel(log);
        this.log = log;
        this.add("IC IDL Management", idlManagementPanel);
        this.add("IC Identity Management", identityMgmtPanel);
    }

    /**
     * Gets called whenever a new canister is added to the cache. This requires a refresh of the UI.
    */
    public void onCacheLoad() {
        idlManagementPanel.onCacheLoad();
    }
}
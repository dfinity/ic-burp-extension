package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;


public class FileChooserListener implements ActionListener {

    private final Logging log;
    private final IDLManagementPanel idlManagementPanel;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;

    public FileChooserListener(Logging log, IDLManagementPanel idlManagementPanel, AsyncLoadingCache<String, CanisterCacheInfo>  canisterInterfaceCache) {
        this.log = log;
        this.idlManagementPanel = idlManagementPanel;
        this.canisterInterfaceCache = canisterInterfaceCache;
    }

    public void actionPerformed(ActionEvent evt) {
        log.logToOutput("FileChooseListener.actionPerformed: " + evt);

        JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

        int r = j.showOpenDialog(idlManagementPanel);
        log.logToOutput("Opening file...");

        if (r == JFileChooser.APPROVE_OPTION) {
            log.logToOutput("APPROVE_OPTION selected.");
            File file = j.getSelectedFile();
            log.logToOutput("File obtained.");
            log.logToOutput("File path: " + file.getAbsolutePath());
            try {
                String idl = Files.readString(file.toPath());
                log.logToOutput("IDL content: " + idl);

                Optional<String> cid = idlManagementPanel.getSelectedCID();
                log.logToOutput("CID selected for file upload: " + cid);

                if(cid.isEmpty()){
                    log.logToError("No canister selected.");
                    JOptionPane.showMessageDialog(idlManagementPanel, "No canisted selected. Please select a canister from the list.",
                            "No canister Selected", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                canisterInterfaceCache.getIfPresent(cid.get()).get().putCanisterInterface(idl, InterfaceType.MANUAL);

                idlManagementPanel.reloadIDLTable();
            }
            catch (Exception e){
               log.logToError("Error occurred during file selection: " + e);
            }

        }
    }
}
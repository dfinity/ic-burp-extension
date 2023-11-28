package org.dfinity.ic.burp.UI.IDL;

import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.UI.ICButton;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Optional;

public class IDLPanel extends JPanel{


    private final Logging log;
    private final JTextArea idlTextArea;
    private final JTable idlTable;

    public IDLPanel(Logging log, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, IDLManagementPanel idlManagementPanel) {
        this.log = log;
        JPanel left = new JPanel();

        JLabel idlLabel = new JLabel("IDL");
        Font boldFont = idlLabel.getFont();
        boldFont = boldFont.deriveFont(boldFont.getStyle() | Font.BOLD);
        idlLabel.setFont(boldFont);
        left.add(idlLabel);
        left.add(Box.createRigidArea(new Dimension(0, 5)));

        idlTable = new JTable(new IDLTableModel(log, canisterInterfaceCache, idlManagementPanel));
        IDLSelectionListener idlSelectionListener = new IDLSelectionListener(log, idlManagementPanel);
        idlTable.getSelectionModel().addListSelectionListener(idlSelectionListener);
        idlTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        idlTable.setTableHeader(null);

        left.add(idlTable);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(Box.createRigidArea(new Dimension(0, 15)));

        FileChooserListener fileChooserListener = new FileChooserListener(log, idlManagementPanel, canisterInterfaceCache);
        left.add(new ICButton(log, "Load IDL", fileChooserListener));
        left.add(Box.createRigidArea(new Dimension(0, 5)));
        SetActiveListener setActiveListener = new SetActiveListener(log, idlManagementPanel, canisterInterfaceCache);
        left.add(new ICButton(log, "Set as active", setActiveListener));

        JPanel right = new JPanel();
        idlTextArea = new JTextArea("IDL CONTENT");
        JLabel idlContentLabel = new JLabel("IDL Content");

        idlContentLabel.setFont(boldFont);
        idlContentLabel.setHorizontalAlignment(SwingConstants.LEFT);

        right.add(idlContentLabel);
        right.add(Box.createRigidArea(new Dimension(0, 5)));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JScrollPane idlTextAreaScroll = new JScrollPane(idlTextArea);

        idlTextAreaScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(idlTextAreaScroll);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(200);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(split);
    }

    public Optional<InterfaceType> getSelectedType(){
        if(idlTable.getSelectedRow() < 0 || idlTable.getSelectedColumn() < 0){
            return Optional.empty();
        }
        Optional<Object> val = Optional.ofNullable(idlTable.getValueAt(idlTable.getSelectedRow(), idlTable.getSelectedColumn()));
        if (val.isEmpty()) {
            return Optional.empty();
        }
        // TODO, very ugly way to go from the value at (which is a String that was manipulated) to the type.
        String valString = val.get().toString();
        InterfaceType t = InterfaceType.valueOf(valString.split(" ")[0]);
        log.logToOutput("getSelectedType return type: " + t);
        log.logToOutput("getSelectedType split the value into: " + valString.split(" ")[0]);
        return Optional.of(t);
    }

    public void setIDLContent(String idl) {
        idlTextArea.setText(idl);
        idlTextArea.setCaretPosition(0);
        idlTextArea.revalidate();
        idlTextArea.repaint();
    }

    public void reloadIDLTable() {
        log.logToOutput("IDLPanel.reloadIDLTable");
        AbstractTableModel m = (AbstractTableModel) idlTable.getModel();
        m.fireTableDataChanged();
    }
}

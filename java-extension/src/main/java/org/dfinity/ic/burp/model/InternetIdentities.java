package org.dfinity.ic.burp.model;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.RequestInfo;

import javax.swing.table.AbstractTableModel;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

public class InternetIdentities extends AbstractTableModel {

    private final IcTools tools;
    private final Logging log;
    // Maps anchor (Integer) onto pem files (String)
    HashMap<String, InternetIdentity> identities = new HashMap<>();

    public InternetIdentities(Logging log, IcTools tools){
        this.log = log;
        this.tools = tools;

        // TODO Remove after UI testing. For testing purposes, add some dummy data.
        try {
            identities.put("123456", new InternetIdentity("Anchor", tools));
            identities.put("654321", new InternetIdentity("Anchor", tools));
        } catch (IcToolsException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> addIdentity(String anchor) throws IcToolsException {
        /*
        String pem = IcTools.generateEd25519Key();
        Identity identity = Identity.ed25519Identity(pem);
        String code = tools.iiAddPasskey(anchor, identity);
        identities.put(anchor, pem);
        */
        if(identities.containsKey(anchor))
            return Optional.empty();
        InternetIdentity ii = new InternetIdentity(anchor, tools);
        identities.put(anchor, ii);
        int rowAdded = identities.keySet().stream().sorted().toList().indexOf(anchor);
        this.fireTableRowsInserted(rowAdded,rowAdded);
        return ii.getCode();
    }

    /*
    public boolean pollIdentity(Integer anchor){
        String pem = identities.get(anchor);
        if(pem == null){
            return false;
        }
        return tools.iiIsPasskeyRegistered(anchor, Identity.ed25519Identity(pem));
    }*/

    /**
     * This method returns the correct identity that should be used to re-sign the request.
     * It looks if the x-ic-sign-identity header is present, it verifies whether an identity with this anchor is available
     * and generates an identity from the pem file for this identity.
     *
     * If no such header exists, we go through the list of all InternetIdentities to find one that results in the same
     * principal as the one found in the sender field of the request.
     *
     * Otherwise, we return an error.
     * @param requestInfo
     * TODO, maybe we need to add the header as param as well.
     * @return
     * TODO Should we call iiIsPasskeyRegistered to verify that the passkey is still valid?
     */
    public Optional<Identity> findIdentity(RequestInfo requestInfo){
        /*for(Map.Entry<Integer, String> entry : identities.entrySet()){
            Identity id = Identity.ed25519Identity(entry.getValue());
            Principal principal = IcTools.iiGetPrincipal(entry.getKey(), id, request.Origin);
            if (principal == requestInfo.senderInfo().sender()){
                Object sessionKey = IcTools.generateEd25519Key();
                Object delegationInfo = IcTools.iiGetDelegation(entry.getKey(), id, request.Origin, sessionKey);
                return Optional.of(Identity.delegatedEd25519Identity(sessionKey, delegationInfo.pubkey, delegationInfo.delegation));
            }
        }*/
        return Optional.empty();
    }

    @Override
    public int getRowCount() {
        return identities.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> "Anchor";
            case 1 -> "Code";
            case 2 -> "Creation date";
            case 3 -> "Is Active";
            case 4 -> "Activation date";
            default -> "Out of range";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> String.class;
            case 1 -> String.class;
            case 2 -> Date.class;
            case 3 -> Boolean.class;
            case 4 -> Date.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(rowIndex > this.identities.size()){
            return "Error fetching identity.";
        }
        String anchor = this.identities.keySet().stream().sorted().toList().get(rowIndex);
        InternetIdentity ii = this.identities.get(anchor);
        if(ii == null){
            this.log.logToError("Could not retrieve II in getValueAt.");
            return "";
        }
        return switch (columnIndex) {
            case 0 -> anchor;
            case 1 -> ii.getCode().orElse("");
            case 2 -> ii.creationDate();
            case 3 -> ii.isActive();
            case 4 -> ii.activationDate().orElse(new Date(0));
            default -> "Requesting column out of range.";
        };
    }
}

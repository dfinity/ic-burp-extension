package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestInfo;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.HashMap;
import java.util.Optional;

public class InternetIdentities extends AbstractTableModel {

    private final IcTools tools;
    // Maps anchor (Integer) onto pem files (String)
    HashMap<Integer, InternetIdentity> identities = new HashMap<>();

    public InternetIdentities(IcTools tools){
        this.tools = tools;

        // TODO Remove after UI testing. For testing purposes, add some dummy data.
        identities.put(123456, "PEM CONTENTS 1");
        identities.put(654321, "PEM CONTENTS 2");

    }


    public String addIdentity(Integer anchor){
        /*
        String pem = IcTools.generateEd25519Key();
        Identity identity = Identity.ed25519Identity(pem);
        String code = tools.iiAddPasskey(anchor, identity);
        identities.put(anchor, pem);
        */
        int rowAdded = identities.keySet().stream().sorted().toList().indexOf(anchor);
        this.fireTableRowsInserted(rowAdded,rowAdded);
        return "code";
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
        return 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "Anchor";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Integer.class;
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

        Integer anchor = this.identities.keySet().stream().sorted().toList().get(rowIndex);
        return anchor;
    }
}

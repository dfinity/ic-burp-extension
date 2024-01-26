package org.dfinity.ic.burp.model;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestSenderInfo;

import javax.swing.table.AbstractTableModel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InternetIdentities extends AbstractTableModel {

    private final IcTools tools;
    private final Logging log;
    // Maps anchor (Integer) onto pem files (String)
    HashMap<String, InternetIdentity> identities = new HashMap<>();

    private Optional<String> selectedII;

    public InternetIdentities(Logging log, IcTools tools){
        this.log = log;
        this.tools = tools;
        this.selectedII = Optional.empty();
    }

    public Optional<InternetIdentity> addIdentity(String anchor) throws IcToolsException {
        if(anchor == null) return Optional.empty();

        if(identities.containsKey(anchor))
            return Optional.empty();
        InternetIdentity ii = new InternetIdentity(anchor, tools);
        identities.put(anchor, ii);
        int rowAdded = identities.keySet().stream().sorted().toList().indexOf(anchor);
        this.fireTableRowsInserted(rowAdded,rowAdded);
        return Optional.of(ii);
    }


    public boolean checkActivations() {
        boolean r = true;
        for(Map.Entry<String, InternetIdentity> entry : identities.entrySet()) {
            InternetIdentity id = entry.getValue();
            try {
                id.checkActivation();
            } catch (IcToolsException e) {
                r = false;
            }
        }
        return r;
    }

    /**
     * This method returns the correct identity that should be used to re-sign the request.
     * It looks if the x-ic-sign-identity header is present, it verifies whether an identity with this anchor is available
     * and generates an identity from the pem file for this identity.
     *
     * If no such header exists, we go through the list of all InternetIdentities to find one that results in the same
     * principal as the one found in the sender field of the request.
     *
     * Otherwise, we return an error.
     * @return
     * TODO Should we call iiIsPasskeyRegistered to verify that the passkey is still valid?
     */
    public Optional<String> findAnchor(RequestSenderInfo requestSenderInfo, String origin) {
        if(requestSenderInfo.sender().equals(Principal.anonymous())){
            return Optional.of("anonymous");
        }

        if(origin == null) return Optional.empty();

        for(Map.Entry<String, InternetIdentity> entry : identities.entrySet()){
            InternetIdentity ii = entry.getValue();
            if(!ii.getState().equals(IiState.Active)){
                continue;
            }
            try {
                Principal p = this.tools.internetIdentityGetPrincipal(entry.getKey(), ii.getPasskey(), origin);
                if (p.equals(requestSenderInfo.sender())){
                    return Optional.of(ii.getAnchor());
                }
            }
            catch (IcToolsException e) {
                // SignIdentity might not be registered as passkey for this II.
                continue;
            }
        }
        return Optional.empty();
    }

    public Optional<Identity> findSignIdentity(String anchor, String origin) throws IcToolsException {
        if(anchor.equals("anonymous")){
            return Optional.of(Identity.anonymousIdentity());
        }
        Optional<InternetIdentity> ii = this.getIdentity(anchor);
        if(ii.isEmpty()) return Optional.empty();
        return ii.get().getSignIdentity(origin);
    }

    private Optional<InternetIdentity> getIdentity(String anchor) {
        return Optional.ofNullable(this.identities.get(anchor));
    }


    public boolean reactivateSelected() throws IcToolsException {
        InternetIdentity ii = getSelectedII();
        if (ii == null)
            return false;

        ii.reactivate();

        return true;
    }

    public boolean removeSelected() {
        if(this.selectedII.isEmpty()) return false;
        return this.remove(this.selectedII.get());
    }


    public boolean remove(String anchor) {
        return this.identities.remove(anchor) != null;
    }

    public void setSelectedIiAnchor(Optional<String> anchor){
        this.selectedII = anchor;
    }

    public InternetIdentity getSelectedII(){
        if(this.selectedII.isEmpty()) return null;

        InternetIdentity ii = this.identities.get(this.selectedII.get());
        return ii;
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
            case 3 -> "State";
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
            case 3 -> String.class;
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
            case 3 -> ii.getState().toString();
            case 4 -> ii.activationDate().orElse(new Date(0));
            default -> "Requesting column out of range.";
        };
    }
}

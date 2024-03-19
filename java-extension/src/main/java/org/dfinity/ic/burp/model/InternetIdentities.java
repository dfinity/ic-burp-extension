package org.dfinity.ic.burp.model;

import burp.api.montoya.logging.Logging;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.jna.model.JnaIdentityInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.Principal;

import javax.swing.table.AbstractTableModel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class InternetIdentities extends AbstractTableModel {

    private final IcTools tools;
    private final Logging log;
    // Maps principals discovered in get_delegation messages to anchor and frontendHostname. To avoid a dependency on JavaFx we use
    // a list instead of Pair. The first element in the list is the anchor and the second is the frontendHostname.
    private final Map<Principal, List<String>> principalToAnchorMap;

    // Maps anchor (Integer) onto pem files (String)
    HashMap<String, InternetIdentity> identities = new HashMap<>();
    private Optional<String> selectedII;

    public InternetIdentities(Logging log, IcTools tools) {
        this.log = log;
        this.tools = tools;
        this.selectedII = Optional.empty();
        this.principalToAnchorMap = new HashMap<>();
    }

    public HashMap<String, InternetIdentity> getIdentities() {
        return identities;
    }

    public Optional<InternetIdentity> addIdentity(String anchor) throws IcToolsException {
        if (anchor == null) return Optional.empty();
        anchor = anchor.toLowerCase();

        if (identities.containsKey(anchor))
            return Optional.empty();
        InternetIdentity ii = new InternetIdentity(anchor, tools, log);
        identities.put(anchor, ii);
        int rowAdded = identities.keySet().stream().sorted().toList().indexOf(anchor);
        this.fireTableRowsInserted(rowAdded, rowAdded);
        return Optional.of(ii);
    }

    /**
     * Used to create an existing II from storage.
     *
     * @param anchor         The anchor used for this II.
     * @param passKeyPem     The private key of the passKey authorized for this II.
     * @param creationDate   When the II was initially added to BurpSuite.
     * @param activationDate When the II was activated the last time by authorizing the passkey.
     */
    public void addIdentity(String anchor, String passKeyPem, IiState state, Date creationDate, Date activationDate) throws IcToolsException {
        InternetIdentity ii = new InternetIdentity(anchor, this.tools, this.log, state, passKeyPem, creationDate, activationDate);
        identities.put(anchor, ii);
    }


    public boolean checkActivations() {
        boolean r = true;
        for (Map.Entry<String, InternetIdentity> entry : identities.entrySet()) {
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
     * This method tries to find the anchor that corresponds to the sender principal.
     *
     * @param sender           The requestSenderInfo from the request being sent.
     * @param frontendHostname The frontendHostname header which is the default hostname used to generate a session key. Some dApps use an
     *                         alternative frontendHostname. In that case, we should have seen a get_delegation message containing that frontendHostname
     *                         and the anchor. This information is stored in `principalToAnchorMap`.
     * @return Returns a list with two elements. The first is the anchor. The second is the frontendHostname/hostname to be used
     * to obtain a session key for the same principal as found in the requestSenderInfo.
     */
    public Optional<List<String>> findAnchor(Principal sender, String frontendHostname) {
        if (sender.equals(Principal.anonymous())) {
            return Optional.of(new ArrayList<>(Arrays.asList("anonymous", frontendHostname)));
        }

        if (frontendHostname == null) return Optional.empty();
        if (principalToAnchorMap.get(sender) != null) {
            return Optional.of(principalToAnchorMap.get(sender));
        }

        for (Map.Entry<String, InternetIdentity> entry : identities.entrySet()) {
            InternetIdentity ii = entry.getValue();

            if (!ii.getState().equals(IiState.Active)) {
                continue;
            }
            try {
                Principal p = this.tools.internetIdentityGetPrincipal(entry.getKey(), ii.getPasskey(), frontendHostname);
                if (p.equals(sender)) {
                    return Optional.of(new ArrayList<>(Arrays.asList(ii.getAnchor(), frontendHostname)));
                }
            } catch (IcToolsException e) {
                // SignIdentity might not be registered as passkey for this II.
                this.log.logToError("An exception occurred trying to find the principal for the  anchor" + entry.getKey()
                        + "\n" + e.getStackTraceAsString());
            }
        }
        return Optional.empty();
    }


    /**
     * Obtain the Identity which can be used to sign the outgoing request which maintain the same InternetIdentity and thus
     * the same principal.
     *
     * @param anchor           The anchor for which to get the
     * @param frontendHostname The hostname to use to derive the s
     * @return The identity including a delegation for the session key for the II linked to the anchor for the given frontendHostname. This Identity can be used to re-sign outgoing requests.
     * @throws IcToolsException If an error occurs during delegation issuing, e.g., the signIdentity is not registered as passkey for the given anchor
     */
    public Optional<Identity> findSignIdentity(String anchor, String frontendHostname) throws IcToolsException {
        anchor = anchor.toLowerCase();
        if (anchor.equals("anonymous")) {
            return Optional.of(Identity.anonymousIdentity());
        }
        Optional<InternetIdentity> ii = this.getIdentity(anchor);
        if (ii.isEmpty()) return Optional.empty();
        return ii.get().getSignIdentity(frontendHostname);
    }

    public void updatePrincipalToAnchorMap(String anchor, String frontendHostname) {
        Optional<InternetIdentity> ii = this.getIdentity(anchor);
        if (ii.isPresent()) {
            try {
                Principal p = this.tools.internetIdentityGetPrincipal(anchor, ii.get().getPasskey(), frontendHostname);
                List<String> val = new ArrayList<>();
                val.add(anchor);
                val.add(frontendHostname);
                log.logToOutput("Adding to principalToAnchorMap: " + p.toString() + " and " + val);
                // The principal should be unique for every anchor/frontendHostname combination. Hence, we shouldn't risk collision in this map.
                this.principalToAnchorMap.put(p, val);
            } catch (IcToolsException e) {
                log.logToError("Error occurred trying to update the principal to anchor map: " + e.getStackTraceAsString());
            }
        }
    }

    private Optional<InternetIdentity> getIdentity(String anchor) {
        anchor = anchor.toLowerCase();
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
        return this.selectedII.filter(this::remove).isPresent();
    }

    public boolean remove(String anchor) {
        return this.identities.remove(anchor) != null;
    }

    public void setSelectedIiAnchor(Optional<String> anchor) {
        this.selectedII = anchor;
    }

    public InternetIdentity getSelectedII() {
        return this.selectedII.map(s -> this.identities.get(s)).orElse(null);
    }

    public String getDelegation(String frontendHostname) {
        if (selectedII.isEmpty() || frontendHostname.isBlank() || !identities.containsKey(selectedII.get()))
            return null;
        InternetIdentity ii = identities.get(selectedII.get());
        Optional<Identity> optId = ii.getSignIdentity(frontendHostname);
        if (optId.isEmpty() || optId.get().delegationTarget().isEmpty())
            return null;
        Identity id = optId.get();
        if (id.delegationFromPubKey().isEmpty() || id.delegationChain().isEmpty() || id.delegationTarget().isEmpty() || id.delegationTarget().get().pem.isEmpty())
            return null;

        JnaIdentityInfo identityInfo = JnaIdentityInfo.from(id);
        if (identityInfo.pem == null || identityInfo.delegation_chain == null || identityInfo.delegation_from_pubkey == null)
            return null;

        return Base64.getEncoder().withoutPadding().encodeToString((identityInfo.identity_type + "|" + identityInfo.pem + "|" + identityInfo.delegation_from_pubkey + "|" + identityInfo.delegation_chain).getBytes(StandardCharsets.UTF_8));
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex > this.identities.size()) {
            return "Error fetching identity.";
        }
        String anchor = this.identities.keySet().stream().sorted().toList().get(rowIndex);
        InternetIdentity ii = this.identities.get(anchor);
        if (ii == null) {
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

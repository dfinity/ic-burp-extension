package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;

import java.util.Date;
import java.util.Optional;

public class InternetIdentity {
    private final String anchor;
    private final IcTools icTools;
    // Long-term ED25519 key which is added as passkey to the internet identity.
    private Identity passkey;
    // Short term ED25519 key that gets generated to re-sign a request. A delegation is obtained for this identity so
    // that this key represents the II anchor. This key and delegation needs to be generated for every origin and expires
    // by default after 30 minutes.
    private Optional<Identity> sessionKey;
    private Optional<String> code;
    private final Date creationDate;
    private Optional<Date> activationDate;
    // Keeps track whether the passkey was added to the II by the user. It is possible this boolean is set to false
    // if it is detected that the passkey is no longer valid.
    private IiState state;

    public InternetIdentity(String anchor, IcTools tools) throws IcToolsException {
        this.anchor = anchor;
        this.icTools = tools;
        this.passkey = Identity.ed25519Identity(tools.generateEd25519Key());
        this.sessionKey = Optional.empty();
        this.code = Optional.ofNullable(tools.internetIdentityAddTentativePasskey(anchor, this.passkey));
        this.creationDate = new Date();
        this.activationDate = Optional.empty();

        // If a code was fetched, we are in the CodeObtained state, otherwise we are still in the initial state.
        this.state = code.isEmpty() ? IiState.Initial : IiState.CodeObtained;
    }

    public String getAnchor() {
        return anchor;
    }

    /**
     *
     * @return The activation code if it has been requested with internetIdentityAddTentativePasskey during initialization.
     */
    public Optional<String> getCode() {
        return this.code;
    }

    /**
     * This function updates isActive by polling the status of the passkey with the II canister.
     * It should not be called too often as it leads to a query call to the IC.
     */
    public void checkActivation() throws IcToolsException {
        if(this.state == IiState.Active) {
            this.state = icTools.internetIdentityIsPasskeyRegistered(this.anchor, this.passkey) ? IiState.Active : IiState.Deactivated;
        }
    }

    public Date creationDate() {
        return this.creationDate;
    }

    public Optional<Date> activationDate() {
        return this.activationDate;
    }

    public IiState getState() {
        if(this.state.equals(IiState.CodeObtained)){
            try {
                // TODO Make async as this is now a long blocking call. The moment one of the IIs is in CodeObtained state, the UI is slow.
                if(icTools.internetIdentityIsPasskeyRegistered(this.anchor, this.passkey)){
                    this.state = IiState.Active;
                    this.activationDate = Optional.of(new Date());
                    this.code = Optional.empty();
                }
            } catch (IcToolsException e) {

            }
        }
        return this.state;
    }

    public void reactivate() throws IcToolsException {
        this.passkey = Identity.ed25519Identity(this.icTools.generateEd25519Key());
        this.code = Optional.ofNullable(icTools.internetIdentityAddTentativePasskey(anchor, this.passkey));
        this.activationDate = Optional.empty();
        this.state = IiState.CodeObtained;
    }
}

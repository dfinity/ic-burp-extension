package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;

import javax.swing.text.html.Option;
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
    private Boolean active;
    private IiState state;

    private enum IiState{
        Initial,
        CodeObtained,
        Active,
        Deactivated
    }

    public InternetIdentity(String anchor, IcTools tools) throws IcToolsException {
        this.anchor = anchor;
        this.icTools = tools;
        this.passkey = Identity.ed25519Identity(tools.generateEd25519Key());
        this.sessionKey = Optional.empty();
        this.code = Optional.ofNullable(tools.internetIdentityAddTentativePasskey(anchor, this.passkey));
        this.creationDate = new Date();
        this.activationDate = Optional.empty();
        this.active = false;
        this.state = code.isEmpty() ? IiState.Initial : IiState.CodeObtained;
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
        this.active = icTools.internetIdentityIsPasskeyRegistered(this.anchor, this.passkey);
        if(!this.active){
            this.activationDate = Optional.empty();
        }
    }


    public boolean isActive() {
        if(!this.active) {
            try {
                this.active = icTools.internetIdentityIsPasskeyRegistered(this.anchor, this.passkey);
                if(this.active){
                    this.activationDate = Optional.of(new Date());
                    this.code = Optional.empty();
                }
            } catch (IcToolsException e){
                return this.active;
            }
        }
        return this.active;
    }

    public Date creationDate() {
        return this.creationDate;
    }

    public Optional<Date> activationDate() {
        return this.activationDate;
    }

    public void reactivate() {
        // TODO Implement
    }
}

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

    public InternetIdentity(String anchor, IcTools tools) throws IcToolsException {
        this.anchor = anchor;
        this.icTools = tools;
        this.passkey = Identity.ed25519Identity(tools.generateEd25519Key());
        this.sessionKey = Optional.empty();
        this.code = Optional.ofNullable(tools.internetIdentityAddTentativePasskey(anchor, this.passkey));
        this.creationDate = new Date();
        this.activationDate = Optional.empty();
        this.active = false;
    }

    public Optional<String> getCode() {
        try {
            if (this.code.isEmpty()) {
                this.code = Optional.ofNullable(icTools.internetIdentityAddTentativePasskey(this.anchor, this.passkey));
            }
        }
        catch(IcToolsException e){
            return Optional.empty();
        }
        return this.code;
    }

    public boolean isActive() {
        if(!this.active) {
            try {
                this.active = icTools.internetIdentityIsPasskeyRegistered(this.anchor, this.passkey);
                if(this.active){
                    this.activationDate = Optional.of(new Date());
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
}

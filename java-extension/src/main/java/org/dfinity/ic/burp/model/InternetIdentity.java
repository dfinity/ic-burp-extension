package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.model.Identity;

import java.util.Date;
import java.util.Optional;

public class InternetIdentity {
    private final Integer anchor;
    // Longterm ED25519 key which is added as passkey to the internet identity.
    private Optional<Identity> passkey;
    // Short term ED25519 key that gets generated to re-sign a request. A delegation is obtained for this identity so
    // that this key represents the II anchor. This key and delegation needs to be generated for every origin and expires
    // by default after 30 minutes.
    private Identity sessionKey;
    private String code;
    private final Date creationDate;
    private Date activationDate;
    // Keeps track whether the passkey was added to the II by the user. It is possible this boolean is set to false
    // if it is detected that the passkey is no longer valid.
    private Boolean active;

    public InternetIdentity(Integer anchor) {
        this.anchor = anchor;
        this.creationDate = new Date();
    }
}

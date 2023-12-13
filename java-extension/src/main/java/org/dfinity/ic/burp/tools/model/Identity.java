package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

public class Identity {
    public final IdentityType type;
    public final Optional<String> pem;

    private boolean active;
    private String label;
    private String pubKey;

    private Identity(IdentityType type, Optional<String> pem) {
        this.type = type;
        this.pem = pem;
    }

    public static Identity anonymousIdentity() {
        // Anonymous identity means no signature is created
        return new Identity(IdentityType.ANONYMOUS, Optional.empty());
    }

    public static Identity ed25519Identity(String pem) {
        // PEM file must contain the PKCS#8 v2 encoded Ed25519 private key
        return new Identity(IdentityType.ED25519, Optional.of(pem));
    }

    public static Identity secp256k1Identity(String pem) {
        // PEM file must contain the SEC1 ASN.1 DER encoded ECPrivateKey
        return new Identity(IdentityType.SECP256K1, Optional.of(pem));
    }

    public boolean isActive() {
        return active;
    }
}

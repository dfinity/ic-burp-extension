package org.dfinity.ic.burp.tools.model;

import java.util.List;
import java.util.Optional;

public class Identity {
    public final IdentityType type;
    public final Optional<String> pem;
    private final Optional<DelegationInfo> delegationInfo;

    private Identity(IdentityType type, Optional<String> pem, Optional<DelegationInfo> delegationInfo) {
        this.type = type;
        this.pem = pem;
        this.delegationInfo = delegationInfo;
    }

    public static Identity anonymousIdentity() {
        // Anonymous identity means no signature is created
        return new Identity(IdentityType.ANONYMOUS, Optional.empty(), Optional.empty());
    }

    public static Identity ed25519Identity(String pem) {
        // PEM file must contain the PKCS#8 v2 encoded Ed25519 private key
        return new Identity(IdentityType.ED25519, Optional.of(pem), Optional.empty());
    }

    public static Identity delegatedEd25519Identity(String pem, String fromPubKey, List<RequestSenderDelegation> delegationChain) {
        // PEM file must contain the private key that corresponds to the public key specified in the last delegation of the delegation chain
        // fromPubKey must be base64nopad encoded public key that signs the first delegation
        return new Identity(IdentityType.DELEGATED, Optional.empty(), Optional.of(new DelegationInfo(ed25519Identity(pem), fromPubKey, delegationChain)));
    }

    public static Identity secp256k1Identity(String pem) {
        // PEM file must contain the SEC1 ASN.1 DER encoded ECPrivateKey
        return new Identity(IdentityType.SECP256K1, Optional.of(pem), Optional.empty());
    }

    public static Identity delegatedSecp256k1Identity(String pem, String fromPubKey, List<RequestSenderDelegation> delegationChain) {
        // PEM file must contain the private key that corresponds to the public key specified in the last delegation of the delegation chain
        // fromPubKey must be base64nopad encoded public key that signs the first delegation
        return new Identity(IdentityType.DELEGATED, Optional.empty(), Optional.of(new DelegationInfo(secp256k1Identity(pem), fromPubKey, delegationChain)));
    }

    public Optional<Identity> delegationTarget() {
        return delegationInfo.map(DelegationInfo::delegationTarget);
    }

    public Optional<String> delegationFromPubKey() {
        return delegationInfo.map(DelegationInfo::fromPubKey);
    }

    public Optional<List<RequestSenderDelegation>> delegationChain() {
        return delegationInfo.map(DelegationInfo::delegationChain);
    }

    private record DelegationInfo(Identity delegationTarget, String fromPubKey,
                                  List<RequestSenderDelegation> delegationChain) {
    }
}

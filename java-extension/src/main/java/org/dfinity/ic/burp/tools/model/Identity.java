package org.dfinity.ic.burp.tools.model;

import java.util.List;
import java.util.Optional;

public class Identity {
    public final IdentityType type;
    public final Optional<String> pem;
    private final Optional<InternalDelegationInfo> delegationInfo;

    private Identity(IdentityType type, Optional<String> pem, Optional<InternalDelegationInfo> delegationInfo) {
        this.type = type;
        this.pem = pem;
        this.delegationInfo = delegationInfo;
    }

    /**
     * Create a new anonymous identity.
     *
     * @return created identity
     */
    public static Identity anonymousIdentity() {
        // Anonymous identity means no signature is created
        return new Identity(IdentityType.ANONYMOUS, Optional.empty(), Optional.empty());
    }

    /**
     * Creates a new Ed25519 Identity without delegation.
     *
     * @param pem must contain the PKCS#8 v2 encoded Ed25519 private key
     * @return created identity
     */
    public static Identity ed25519Identity(String pem) {
        return new Identity(IdentityType.ED25519, Optional.of(pem), Optional.empty());
    }

    /**
     * Creates a new delegated Ed25519 Identity.
     *
     * @param pem            must contain the private key that corresponds to the Ed25519 public key specified in the last delegation of the delegation chain
     * @param delegationInfo information about the delegation
     * @return created identity
     */
    public static Identity delegatedEd25519Identity(String pem, DelegationInfo delegationInfo) {
        return new Identity(IdentityType.DELEGATED, Optional.empty(), Optional.of(new InternalDelegationInfo(ed25519Identity(pem), delegationInfo)));
    }

    /**
     * Creates a new Secp256k1 Identity without delegation.
     *
     * @param pem must contain the SEC1 ASN.1 DER encoded ECPrivateKey
     * @return created identity
     */
    public static Identity secp256k1Identity(String pem) {
        return new Identity(IdentityType.SECP256K1, Optional.of(pem), Optional.empty());
    }

    /**
     * Creates a new delegated Secp256k1 Identity.
     *
     * @param pem            must contain the private key that corresponds to the Secp256k1 public key specified in the last delegation of the delegation chain
     * @param delegationInfo information about the delegation
     * @return created identity
     */
    public static Identity delegatedSecp256k1Identity(String pem, DelegationInfo delegationInfo) {
        return new Identity(IdentityType.DELEGATED, Optional.empty(), Optional.of(new InternalDelegationInfo(secp256k1Identity(pem), delegationInfo)));
    }

    /**
     * @return the identity that acts on behalf of another identity or an empty optional if no delegations are used
     */
    public Optional<Identity> delegationTarget() {
        return delegationInfo.map(InternalDelegationInfo::delegationTarget);
    }

    /**
     * @return the public key of the identity that delegates to the delegation target or an empty optional if no delegations are used
     */
    public Optional<String> delegationFromPubKey() {
        return delegationInfo.map(x -> x.delegationInfo.fromPubKey());
    }

    /**
     * @return the delegation chain or an empty optional if no delegations are used
     */
    public Optional<List<RequestSenderDelegation>> delegationChain() {
        return delegationInfo.map(x -> x.delegationInfo.delegationChain());
    }

    public Optional<String> getPem() {
        return pem;
    }

    private record InternalDelegationInfo(Identity delegationTarget, DelegationInfo delegationInfo) {
    }
}

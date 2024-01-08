package org.dfinity.ic.burp.tools.model;

import java.util.List;

/**
 * @param fromPubKey      the base64nopad encoded public key that signs the first delegation
 * @param delegationChain the delegation chain
 */
public record DelegationInfo(String fromPubKey, List<RequestSenderDelegation> delegationChain) {
}

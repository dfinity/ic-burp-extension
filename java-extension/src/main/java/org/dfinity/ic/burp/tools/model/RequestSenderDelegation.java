package org.dfinity.ic.burp.tools.model;

import java.util.List;

/**
 * @param pubkey     delegation target
 * @param expiration expiration timestamp in nanoseconds, TODO: change type to BigInteger so it can handle full u64 range
 * @param targets    restricts delegation to canister IDs in that list, if empty no restrictions apply
 * @param signature  signature of the delegation
 */
public record RequestSenderDelegation(String pubkey, long expiration, List<Principal> targets,
                                      String signature) {
}

package org.dfinity.ic.burp.tools.model;

import java.util.List;
import java.util.Optional;

/**
 * @param sender     principal id of the sender
 * @param pubkey     public key of the sender or an empty optional if sender is 2vxsx-fae (anonymous)
 * @param sig        signature of the request or an empty optional if sender is 2vxsx-fae (anonymous)
 * @param delegation list of delegations, might be empty
 */
public record RequestSenderInfo(Principal sender, Optional<String> pubkey, Optional<String> sig,
                                List<RequestSenderDelegation> delegation) {
}

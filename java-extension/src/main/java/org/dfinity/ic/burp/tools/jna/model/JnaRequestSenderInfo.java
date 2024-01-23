package org.dfinity.ic.burp.tools.jna.model;

import org.dfinity.ic.burp.tools.jna.JnaDelegationParser;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestSenderDelegation;
import org.dfinity.ic.burp.tools.model.RequestSenderInfo;

import java.util.List;
import java.util.Optional;

public class JnaRequestSenderInfo {

    public static RequestSenderInfo toRequestSenderInfo(String sender, String pubkey, String sig, String delegation) throws IcToolsException {
        try {
            Optional<String> key = pubkey == null || pubkey.isEmpty() ? Optional.empty() : Optional.of(pubkey);
            Optional<String> si = sig == null || sig.isEmpty() ? Optional.empty() : Optional.of(sig);
            List<RequestSenderDelegation> delegations = JnaDelegationParser.parseDelegations(delegation);
            return new RequestSenderInfo(Principal.fromText(sender), key, si, delegations);
        } catch (RuntimeException e) {
            throw new IcToolsException("could not convert to RequestSenderInfo", e);
        }
    }
}

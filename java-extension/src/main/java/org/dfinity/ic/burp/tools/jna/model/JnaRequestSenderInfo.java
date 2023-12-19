package org.dfinity.ic.burp.tools.jna.model;

import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestSenderDelegation;
import org.dfinity.ic.burp.tools.model.RequestSenderInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JnaRequestSenderInfo {

    private static List<RequestSenderDelegation> parseDelegations(String delegation) {
        var res = new ArrayList<RequestSenderDelegation>();
        Arrays.stream(delegation.split(";")).skip(1).forEach(del -> {
            var fields = del.split(":");
            var pubkey = fields[0];
            var expiration = Long.parseLong(fields[1]);
            var targets = Arrays.stream(fields[2].split(",")).skip(1).map(Principal::new).toList();
            var sig = fields[3];
            res.add(new RequestSenderDelegation(pubkey, expiration, targets, sig));
        });
        return res;
    }

    public static RequestSenderInfo toRequestSenderInfo(String sender, String pubkey, String sig, String delegation) throws IcToolsException {
        try {
            Optional<String> key = pubkey == null || pubkey.isEmpty() ? Optional.empty() : Optional.of(pubkey);
            Optional<String> si = sig == null || sig.isEmpty() ? Optional.empty() : Optional.of(sig);
            List<RequestSenderDelegation> delegations = parseDelegations(delegation);
            return new RequestSenderInfo(new Principal(sender), key, si, delegations);
        } catch (RuntimeException e) {
            throw new IcToolsException("could not convert to RequestSenderInfo", e);
        }
    }
}

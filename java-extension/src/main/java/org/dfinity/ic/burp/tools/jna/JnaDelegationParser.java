package org.dfinity.ic.burp.tools.jna;

import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestSenderDelegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JnaDelegationParser {

    public static List<RequestSenderDelegation> parseDelegations(String delegation) {
        var res = new ArrayList<RequestSenderDelegation>();
        Arrays.stream(delegation.split(";")).skip(1).forEach(del -> {
            var fields = del.split(":");
            var pubkey = fields[0];
            var expiration = Long.parseLong(fields[1]);
            var targets = Arrays.stream(fields[2].split(",")).skip(1).map(Principal::fromText).toList();
            var sig = fields[3];
            res.add(new RequestSenderDelegation(pubkey, expiration, targets, sig));
        });
        return res;
    }
}

package org.dfinity.ic.burp.tools.jna.model;

import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.RequestSenderDelegation;

import java.util.List;

public class JnaIdentityInfo {

    public final String identity_type;
    public final String pem;
    public String delegation_from_pubkey;
    public String delegation_chain;

    public JnaIdentityInfo(String identityType, String pem, String delegationFromPubKey, String delegationChain) {
        this.identity_type = identityType;
        this.pem = pem;
        this.delegation_from_pubkey = delegationFromPubKey;
        this.delegation_chain = delegationChain;
    }

    public static JnaIdentityInfo from(Identity identity) {
        switch (identity.type) {
            case ANONYMOUS -> {
                return new JnaIdentityInfo("ANONYMOUS", null, null, null);
            }
            case ED25519 -> {
                return new JnaIdentityInfo("ED25519", identity.pem.orElseThrow(), null, null);
            }
            case SECP256K1 -> {
                return new JnaIdentityInfo("SECP256K1", identity.pem.orElseThrow(), null, null);
            }
            case DELEGATED -> {
                var res = from(identity.delegationTarget().orElseThrow());
                res.delegation_from_pubkey = identity.delegationFromPubKey().orElseThrow();
                res.delegation_chain = delegationToString(identity.delegationChain().orElseThrow());
                return res;
            }
        }
        throw new RuntimeException("identity has unexpected type: " + identity.type);
    }

    private static String delegationToString(List<RequestSenderDelegation> delegations) {
        var res = new StringBuilder();
        res.append(delegations.size());
        for (var del : delegations) {
            res.append(";");
            res.append(del.pubkey());
            res.append(":");
            res.append(del.expiration());
            res.append(":");
            res.append(del.targets().size());
            for (var t : del.targets()) {
                res.append(",");
                res.append(t.id());
            }
            res.append(":");
            res.append(del.signature());
        }
        return res.toString();
    }
}

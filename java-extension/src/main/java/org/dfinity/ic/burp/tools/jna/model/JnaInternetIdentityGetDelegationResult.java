package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.jna.JnaDelegationParser;
import org.dfinity.ic.burp.tools.model.DelegationInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;

@Structure.FieldOrder({"error_message", "from_pubkey", "delegation"})
public class JnaInternetIdentityGetDelegationResult extends Structure {
    public String error_message;
    public String from_pubkey;
    public String delegation;

    public DelegationInfo getDelegationInfo() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return new DelegationInfo(from_pubkey, JnaDelegationParser.parseDelegations(delegation));
    }

    public static class ByValue extends JnaInternetIdentityGetDelegationResult implements Structure.ByValue {
    }
}

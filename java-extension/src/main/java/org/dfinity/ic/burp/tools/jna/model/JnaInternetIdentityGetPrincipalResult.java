package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Principal;

@Structure.FieldOrder({"error_message", "principal"})
public class JnaInternetIdentityGetPrincipalResult extends Structure {
    public String error_message;
    public String principal;

    public Principal getPrincipal() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return Principal.fromText(principal);
    }

    public static class ByValue extends JnaInternetIdentityGetPrincipalResult implements Structure.ByValue {
    }
}

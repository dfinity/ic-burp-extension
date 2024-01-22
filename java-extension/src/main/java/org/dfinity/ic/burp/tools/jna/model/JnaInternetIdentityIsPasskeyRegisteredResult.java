package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

@Structure.FieldOrder({"error_message", "is_passkey_registered"})
public class JnaInternetIdentityIsPasskeyRegisteredResult extends Structure {
    public String error_message;
    public String is_passkey_registered;

    public boolean isPasskeyRegistered() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return Boolean.parseBoolean(is_passkey_registered);
    }

    public static class ByValue extends JnaInternetIdentityIsPasskeyRegisteredResult implements Structure.ByValue {
    }
}

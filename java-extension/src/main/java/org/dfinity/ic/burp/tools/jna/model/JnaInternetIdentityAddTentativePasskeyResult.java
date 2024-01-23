package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

@Structure.FieldOrder({"error_message", "code"})
public class JnaInternetIdentityAddTentativePasskeyResult extends Structure {
    public String error_message;
    public String code;

    public String getCode() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return code;
    }

    public static class ByValue extends JnaInternetIdentityAddTentativePasskeyResult implements Structure.ByValue {
    }
}

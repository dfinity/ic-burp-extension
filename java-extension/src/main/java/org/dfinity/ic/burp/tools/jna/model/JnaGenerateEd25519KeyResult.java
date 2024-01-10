package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

@Structure.FieldOrder({"error_message", "pem_encoded_key"})
public class JnaGenerateEd25519KeyResult extends Structure {
    public String error_message;
    public String pem_encoded_key;

    public String getPemEncodedKey() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return pem_encoded_key;
    }

    public static class ByValue extends JnaGenerateEd25519KeyResult implements Structure.ByValue {
    }
}

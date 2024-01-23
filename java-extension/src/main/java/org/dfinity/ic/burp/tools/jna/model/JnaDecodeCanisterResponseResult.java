package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

@Structure.FieldOrder({"error_message", "decoded_response"})
public class JnaDecodeCanisterResponseResult extends Structure {
    public String error_message;
    public String decoded_response;

    public String getDecodedResponse() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return decoded_response;
    }

    public static class ByValue extends JnaDecodeCanisterResponseResult implements Structure.ByValue {
    }
}

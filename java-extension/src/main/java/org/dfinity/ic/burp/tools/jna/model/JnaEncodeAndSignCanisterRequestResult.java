package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

import java.util.Base64;

@Structure.FieldOrder({"error_message", "encoded_request"})
public class JnaEncodeAndSignCanisterRequestResult extends Structure {
    public String error_message;
    public String encoded_request;

    public byte[] getEncodedRequest() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return Base64.getDecoder().decode(encoded_request);
    }

    public static class ByValue extends JnaEncodeAndSignCanisterRequestResult implements Structure.ByValue {
    }
}

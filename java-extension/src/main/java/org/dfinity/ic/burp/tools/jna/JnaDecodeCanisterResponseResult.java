package org.dfinity.ic.burp.tools.jna;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.IcTools.IcToolsException;

@Structure.FieldOrder({"is_successful", "error_message", "decoded_response"})
public class JnaDecodeCanisterResponseResult extends Structure {
    public static class ByValue extends JnaDecodeCanisterResponseResult implements Structure.ByValue {}

    public boolean is_successful;
    public String error_message;
    public String decoded_response;

    public String getDecodedResponse() throws IcToolsException {
        if(!is_successful)
            throw new IcToolsException(error_message);
        return decoded_response;
    }
}

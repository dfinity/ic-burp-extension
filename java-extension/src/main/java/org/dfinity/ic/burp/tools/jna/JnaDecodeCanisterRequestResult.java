package org.dfinity.ic.burp.tools.jna;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import org.dfinity.ic.burp.tools.IcTools.IcToolsException;
import org.dfinity.ic.burp.tools.IcTools.RequestInfo;
import org.dfinity.ic.burp.tools.IcTools.RequestType;

import java.util.Optional;


@FieldOrder({"is_successful", "error_message", "request_type", "request_id", "decoded_request", "canister_method"})
public class JnaDecodeCanisterRequestResult extends Structure {

    public static class ByValue extends JnaDecodeCanisterRequestResult implements Structure.ByValue {}

    public boolean is_successful;
    public String error_message;
    public String request_type;
    public String request_id;
    public String decoded_request;
    public String canister_method;

    public RequestInfo toRequestInfo() throws IcToolsException {
        if(!is_successful)
            throw new IcToolsException(error_message);

        Optional<String> method = canister_method == null || canister_method.isEmpty() ? Optional.empty() : Optional.of(canister_method);
        return new RequestInfo(RequestType.from(request_type), request_id, decoded_request, method);
    }
}

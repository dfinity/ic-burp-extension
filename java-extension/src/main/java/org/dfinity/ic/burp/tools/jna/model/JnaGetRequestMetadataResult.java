package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;
import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;

@Structure.FieldOrder({"is_successful", "error_message", "request_type", "request_id", "canister_method"})
public class JnaGetRequestMetadataResult extends Structure {

    public boolean is_successful;
    public String error_message;
    public String request_type;
    public String request_id;
    public String canister_method;

    public RequestMetadata toRequestMetadata() throws IcToolsException {
        if (!is_successful)
            throw new IcToolsException(error_message);

        Optional<String> method = canister_method == null || canister_method.isEmpty() ? Optional.empty() : Optional.of(canister_method);
        return new RequestMetadata(RequestType.from(request_type), request_id, method);
    }

    public static class ByValue extends JnaGetRequestMetadataResult implements Structure.ByValue {
    }
}

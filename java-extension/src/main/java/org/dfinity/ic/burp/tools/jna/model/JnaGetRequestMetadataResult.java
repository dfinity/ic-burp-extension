package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestMetadata;
import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;

@Structure.FieldOrder({"error_message", "request_type", "request_id", "sender", "pubkey", "sig", "delegation", "canister_method"})
public class JnaGetRequestMetadataResult extends Structure {

    public String error_message;
    public String request_type;
    public String request_id;
    public String sender;
    public String pubkey;
    public String sig;
    public String delegation;
    public String canister_method;

    public RequestMetadata toRequestMetadata() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);

        Optional<String> rid = request_id == null || request_id.isEmpty() ? Optional.empty() : Optional.of(request_id);
        Optional<String> method = canister_method == null || canister_method.isEmpty() ? Optional.empty() : Optional.of(canister_method);
        return new RequestMetadata(RequestType.from(request_type), rid, JnaRequestSenderInfo.toRequestSenderInfo(sender, pubkey, sig, delegation), method);
    }

    public static class ByValue extends JnaGetRequestMetadataResult implements Structure.ByValue {
    }
}

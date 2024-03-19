package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Request;
import org.dfinity.ic.burp.tools.model.RequestDecoded;
import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;


@FieldOrder({"error_message", "request_type", "sender", "pubkey", "sig", "delegation", "request_id", "canister_id", "canister_method", "decoded_body"})
public class JnaDecodeCanisterRequestResult extends Structure {

    public String error_message;
    public String request_type;
    public String sender;
    public String pubkey;
    public String sig;
    public String delegation;
    public String request_id;
    public String canister_id;
    public String canister_method;
    public String decoded_body;

    public RequestDecoded toDecodedRequest() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);

        Optional<String> rid = request_id == null || request_id.isEmpty() ? Optional.empty() : Optional.of(request_id);
        Optional<String> cid = canister_id == null || canister_id.isEmpty() ? Optional.empty() : Optional.of(canister_id);
        Optional<String> method = canister_method == null || canister_method.isEmpty() ? Optional.empty() : Optional.of(canister_method);
        return Request.decoded(RequestType.from(request_type), JnaRequestSenderInfo.toRequestSenderInfo(sender, pubkey, sig, delegation), rid, cid, method, decoded_body);
    }

    public static class ByValue extends JnaDecodeCanisterRequestResult implements Structure.ByValue {
    }
}

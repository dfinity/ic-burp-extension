package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.IcToolsException;

import java.util.Optional;

@Structure.FieldOrder({"error_message", "canister_interface"})
public class JnaDiscoverCanisterInterfaceResult extends Structure {
    public String error_message;
    public String canister_interface;

    public Optional<String> getCanisterInterface() throws IcToolsException {
        if (error_message != null)
            throw new IcToolsException(error_message);
        return Optional.ofNullable(canister_interface);
    }

    public static class ByValue extends JnaDiscoverCanisterInterfaceResult implements Structure.ByValue {
    }
}

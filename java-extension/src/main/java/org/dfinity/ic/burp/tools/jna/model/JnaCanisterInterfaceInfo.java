package org.dfinity.ic.burp.tools.jna.model;

import com.sun.jna.Structure;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;

import java.util.Optional;

@Structure.FieldOrder({"canister_interface", "canister_method"})
public class JnaCanisterInterfaceInfo extends Structure {

    public String canister_interface;
    public String canister_method;

    public JnaCanisterInterfaceInfo(String canister_interface, String canister_method) {
        this.canister_interface = canister_interface;
        this.canister_method = canister_method;
    }

    public static JnaCanisterInterfaceInfo from(Optional<CanisterInterfaceInfo> canisterInterfaceInfo) {
        return canisterInterfaceInfo.map(inf -> new JnaCanisterInterfaceInfo(inf.canisterInterface(), inf.canisterMethod())).orElseGet(JnaCanisterInterfaceInfo::unknown);
    }

    public static JnaCanisterInterfaceInfo unknown() {
        return new JnaCanisterInterfaceInfo(null, null);
    }
}

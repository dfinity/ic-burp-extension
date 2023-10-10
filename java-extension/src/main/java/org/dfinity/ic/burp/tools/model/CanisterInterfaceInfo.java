package org.dfinity.ic.burp.tools.model;

public class CanisterInterfaceInfo {
    public final String canisterInterface;
    public final String canisterMethod;

    public CanisterInterfaceInfo(String canisterInterface, String canisterMethod) {
        this.canisterInterface = canisterInterface;
        this.canisterMethod = canisterMethod;
    }
}

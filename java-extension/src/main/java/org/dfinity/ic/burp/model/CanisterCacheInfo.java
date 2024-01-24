package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.HashMap;
import java.util.Optional;

public class CanisterCacheInfo {
    private final HashMap<InterfaceType, String> canisterInterfaces = new HashMap<>();
    private InterfaceType activeCanisterInterfaceType;
    public CanisterCacheInfo(String canisterInterface, InterfaceType type) {
        this.canisterInterfaces.put(type, canisterInterface);
        this.activeCanisterInterfaceType = type;
    }

    public CanisterCacheInfo() {
    }

    public void putCanisterInterface(String canisterInterface, InterfaceType type) {
        this.canisterInterfaces.put(type, canisterInterface);
    }

    public Optional<String> getActiveCanisterInterface(){
        String canisterInterface = canisterInterfaces.get(activeCanisterInterfaceType);

        return canisterInterface.isBlank() ? Optional.empty() : Optional.of(canisterInterface);
    }

    public String getCanisterInterface(InterfaceType t){
        return canisterInterfaces.get(t);
    }

    public InterfaceType getActiveCanisterInterfaceType(){
        return activeCanisterInterfaceType;
    }

    public void setActiveCanisterInterfaceType(InterfaceType type){
        this.activeCanisterInterfaceType = type;
    }

    public HashMap<InterfaceType, String> getCanisterInterfaces(){
        return this.canisterInterfaces;
    }

}

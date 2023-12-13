package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.HashMap;
import java.util.Optional;

public class CanisterCacheInfo {
    private final HashMap<InterfaceType, Optional<String>> canisterInterfaces = new HashMap<>();
    private InterfaceType activeCanisterInterfaceType;
    public CanisterCacheInfo(Optional<String> canisterInterface, InterfaceType type) {
        this.canisterInterfaces.put(type, canisterInterface);
        this.activeCanisterInterfaceType = type;
    }

    public CanisterCacheInfo() {

    }

    public void putCanisterInterface(Optional<String> canisterInterface, InterfaceType type) {
        this.canisterInterfaces.put(type, canisterInterface);
        this.activeCanisterInterfaceType = type;
    }

    public Optional<String> getActiveCanisterInterface(){
        return canisterInterfaces.get(activeCanisterInterfaceType);
    }

    public Optional<String> getCanisterInterface(InterfaceType t){
        return canisterInterfaces.get(t);
    }

    public InterfaceType getActiveCanisterInterfaceType(){
        return activeCanisterInterfaceType;
    }

    public void setActiveCanisterInterfaceType(InterfaceType type){
        this.activeCanisterInterfaceType = type;
    }

    public HashMap<InterfaceType, Optional<String>> getCanisterInterfaces(){
        return this.canisterInterfaces;
    }


}

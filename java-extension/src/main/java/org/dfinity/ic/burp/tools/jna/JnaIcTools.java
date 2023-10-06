package org.dfinity.ic.burp.tools.jna;

import com.sun.jna.*;
import org.dfinity.ic.burp.tools.IcTools;

import java.util.Optional;

public class JnaIcTools implements IcTools {

    public interface CIcTools extends Library {
        String LIB_NAME = "rust_lib";
        CIcTools INSTANCE = Native.load((Platform.isMac() ? "lib" : "") +  LIB_NAME + "." + (Platform.isWindows() ? "dll" : Platform.isMac() ? "dylib" : "so"), CIcTools.class);

        JnaDecodeCanisterRequestResult.ByValue decode_canister_request(Pointer encodedCborRequest, int encodedCborRequestSize, String canisterInterfaceOptional);
        JnaDecodeCanisterResponseResult.ByValue decode_canister_response(Pointer encodedCborResponse, int encodedCborResponseSize, JnaCanisterInterfaceInfo canisterInterfaceInfoOptional);
    }

    @Override
    public Optional<String> discoverCanisterInterface(String canisterId) {
        return Optional.empty();
    }

    @Override
    public RequestInfo decodeCanisterRequest(byte[] encodedCborRequest, Optional<String> canisterInterface) throws IcToolsException {

        try(var ptr = new Memory(encodedCborRequest.length) ) {
            ptr.write(0, encodedCborRequest, 0, encodedCborRequest.length);
            return CIcTools.INSTANCE.decode_canister_request(ptr, encodedCborRequest.length, canisterInterface.orElse(null)).toRequestInfo();
        }
    }

    @Override
    public String decodeCanisterResponse(byte[] encodedCborResponse, Optional<CanisterInterfaceInfo> canisterInterfaceInfo) throws IcToolsException {

        try(var ptr = new Memory(encodedCborResponse.length)) {
            ptr.write(0, encodedCborResponse, 0, encodedCborResponse.length);
            return CIcTools.INSTANCE.decode_canister_response(ptr, encodedCborResponse.length, JnaCanisterInterfaceInfo.from(canisterInterfaceInfo)).getDecodedResponse();
        }
    }

}

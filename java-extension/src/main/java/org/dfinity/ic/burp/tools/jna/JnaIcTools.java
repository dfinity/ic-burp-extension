package org.dfinity.ic.burp.tools.jna;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.jna.model.JnaCanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.jna.model.JnaDecodeCanisterRequestResult;
import org.dfinity.ic.burp.tools.jna.model.JnaDecodeCanisterResponseResult;
import org.dfinity.ic.burp.tools.jna.model.JnaDiscoverCanisterInterfaceResult;
import org.dfinity.ic.burp.tools.jna.model.JnaGetRequestMetadataResult;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestInfo;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

public class JnaIcTools implements IcTools {

    @Override
    public Optional<String> discoverCanisterInterface(String canisterId) throws IcToolsException {
        return CIcTools.INSTANCE.discover_canister_interface(canisterId).getCanisterInterface();
    }

    @Override
    public RequestMetadata getRequestMetadata(byte[] encodedCborRequest) throws IcToolsException {
        try (var ptr = new Memory(encodedCborRequest.length)) {
            ptr.write(0, encodedCborRequest, 0, encodedCborRequest.length);
            return CIcTools.INSTANCE.get_request_metadata(ptr, encodedCborRequest.length).toRequestMetadata();
        }
    }

    @Override
    public RequestInfo decodeCanisterRequest(byte[] encodedCborRequest, Optional<String> canisterInterface) throws IcToolsException {

        try (var ptr = new Memory(encodedCborRequest.length)) {
            ptr.write(0, encodedCborRequest, 0, encodedCborRequest.length);
            return CIcTools.INSTANCE.decode_canister_request(ptr, encodedCborRequest.length, canisterInterface.orElse(null)).toRequestInfo();
        }
    }

    @Override
    public String decodeCanisterResponse(byte[] encodedCborResponse, Optional<CanisterInterfaceInfo> canisterInterfaceInfo) throws IcToolsException {

        try (var ptr = new Memory(encodedCborResponse.length)) {
            ptr.write(0, encodedCborResponse, 0, encodedCborResponse.length);
            return CIcTools.INSTANCE.decode_canister_response(ptr, encodedCborResponse.length, JnaCanisterInterfaceInfo.from(canisterInterfaceInfo)).getDecodedResponse();
        }
    }

    public interface CIcTools extends Library {
        String LIB_NAME = "rust_lib";
        CIcTools INSTANCE = Native.load((Platform.isMac() ? "lib" : "") + LIB_NAME + "." + (Platform.isWindows() ? "dll" : Platform.isMac() ? "dylib" : "so"), CIcTools.class);

        JnaDiscoverCanisterInterfaceResult.ByValue discover_canister_interface(String canisterId);

        JnaGetRequestMetadataResult.ByValue get_request_metadata(Pointer encodedCborRequest, int encodedCborRequestSize);

        JnaDecodeCanisterRequestResult.ByValue decode_canister_request(Pointer encodedCborRequest, int encodedCborRequestSize, String canisterInterfaceOptional);

        JnaDecodeCanisterResponseResult.ByValue decode_canister_response(Pointer encodedCborResponse, int encodedCborResponseSize, JnaCanisterInterfaceInfo canisterInterfaceInfoOptional);
    }

}

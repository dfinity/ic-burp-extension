package org.dfinity.ic.burp.tools.jna;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.jna.model.JnaDecodeCanisterRequestResult;
import org.dfinity.ic.burp.tools.jna.model.JnaDecodeCanisterResponseResult;
import org.dfinity.ic.burp.tools.jna.model.JnaDiscoverCanisterInterfaceResult;
import org.dfinity.ic.burp.tools.jna.model.JnaEncodeAndSignCanisterRequestResult;
import org.dfinity.ic.burp.tools.jna.model.JnaGenerateEd25519KeyResult;
import org.dfinity.ic.burp.tools.jna.model.JnaGetRequestMetadataResult;
import org.dfinity.ic.burp.tools.jna.model.JnaIdentityInfo;
import org.dfinity.ic.burp.tools.jna.model.JnaInternetIdentityAddTentativePasskeyResult;
import org.dfinity.ic.burp.tools.jna.model.JnaInternetIdentityGetDelegationResult;
import org.dfinity.ic.burp.tools.jna.model.JnaInternetIdentityGetPrincipalResult;
import org.dfinity.ic.burp.tools.jna.model.JnaInternetIdentityIsPasskeyRegisteredResult;
import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.DelegationInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.Principal;
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
            return CIcTools.INSTANCE.decode_canister_response(ptr, encodedCborResponse.length, canisterInterfaceInfo.map(CanisterInterfaceInfo::canisterInterface).orElse(null), canisterInterfaceInfo.map(CanisterInterfaceInfo::canisterMethod).orElse(null)).getDecodedResponse();
        }
    }

    @Override
    public String generateEd25519Key() throws IcToolsException {
        return CIcTools.INSTANCE.generate_ed25519_key().getPemEncodedKey();
    }

    @Override
    public byte[] encodeAndSignCanisterRequest(String decodedRequest, Optional<String> canisterInterface, Identity signIdentity) throws IcToolsException {
        var identity = JnaIdentityInfo.from(signIdentity);
        return CIcTools.INSTANCE.encode_and_sign_canister_request(decodedRequest, canisterInterface.orElse(null), identity.identity_type, identity.pem, identity.delegation_from_pubkey, identity.delegation_chain).getEncodedRequest();
    }

    @Override
    public String internetIdentityAddTentativePasskey(String anchor, Identity signIdentity) throws IcToolsException {
        var identity = JnaIdentityInfo.from(signIdentity);
        return CIcTools.INSTANCE.internet_identity_add_tentative_passkey(anchor, identity.identity_type, identity.pem, identity.delegation_from_pubkey, identity.delegation_chain).getCode();
    }

    @Override
    public boolean internetIdentityIsPasskeyRegistered(String anchor, Identity signIdentity) throws IcToolsException {
        var identity = JnaIdentityInfo.from(signIdentity);
        return CIcTools.INSTANCE.internet_identity_is_passkey_registered(anchor, identity.identity_type, identity.pem, identity.delegation_from_pubkey, identity.delegation_chain).isPasskeyRegistered();
    }

    @Override
    public Principal internetIdentityGetPrincipal(String anchor, Identity signIdentity, String frontendHostname) throws IcToolsException {
        var identity = JnaIdentityInfo.from(signIdentity);
        return CIcTools.INSTANCE.internet_identity_get_principal(anchor, identity.identity_type, identity.pem, identity.delegation_from_pubkey, identity.delegation_chain, frontendHostname).getPrincipal();
    }

    @Override
    public DelegationInfo internetIdentityGetDelegation(String anchor, Identity signIdentity, String frontendHostname, Identity sessionIdentity) throws IcToolsException {
        var identity = JnaIdentityInfo.from(signIdentity);
        var sIdentity = JnaIdentityInfo.from(sessionIdentity);
        return CIcTools.INSTANCE.internet_identity_get_delegation(anchor, identity.identity_type, identity.pem, identity.delegation_from_pubkey, identity.delegation_chain, frontendHostname, sIdentity.identity_type, sIdentity.pem, sIdentity.delegation_from_pubkey, sIdentity.delegation_chain).getDelegationInfo();
    }

    public interface CIcTools extends Library {
        String LIB_NAME = "rust_lib";
        CIcTools INSTANCE = Native.load((Platform.isMac() ? "lib" : "") + LIB_NAME + "." + (Platform.isWindows() ? "dll" : Platform.isMac() ? "dylib" : "so"), CIcTools.class);

        JnaDiscoverCanisterInterfaceResult.ByValue discover_canister_interface(String canisterId);

        JnaGetRequestMetadataResult.ByValue get_request_metadata(Pointer encodedCborRequest, int encodedCborRequestSize);

        JnaDecodeCanisterRequestResult.ByValue decode_canister_request(Pointer encodedCborRequest, int encodedCborRequestSize, String canisterInterfaceOptional);

        JnaDecodeCanisterResponseResult.ByValue decode_canister_response(Pointer encodedCborResponse, int encodedCborResponseSize, String canisterInterface, String canisterMethod);

        JnaGenerateEd25519KeyResult.ByValue generate_ed25519_key();

        JnaEncodeAndSignCanisterRequestResult.ByValue encode_and_sign_canister_request(String decodedRequest, String canisterInterfaceOptional, String identityType, String identityPemOpt, String identityDelegationFromPubkeyOpt, String identityDelegationChainOpt);

        JnaInternetIdentityAddTentativePasskeyResult.ByValue internet_identity_add_tentative_passkey(String anchor, String identityType, String identityPemOpt, String identityDelegationFromPubkeyOpt, String identityDelegationChainOpt);

        JnaInternetIdentityIsPasskeyRegisteredResult.ByValue internet_identity_is_passkey_registered(String anchor, String identityType, String identityPemOpt, String identityDelegationFromPubkeyOpt, String identityDelegationChainOpt);

        JnaInternetIdentityGetPrincipalResult.ByValue internet_identity_get_principal(String anchor, String identityType, String identityPemOpt, String identityDelegationFromPubkeyOpt, String identityDelegationChainOpt, String frontendHostname);

        JnaInternetIdentityGetDelegationResult.ByValue internet_identity_get_delegation(String anchor, String identityType, String identityPemOpt, String identityDelegationFromPubkeyOpt, String identityDelegationChainOpt, String frontendHostname, String sessionIdentityType, String sessionIdentityPemOpt, String sessionIdentityDelegationFromPubkeyOpt, String sessionIdentityDelegationChainOpt);
    }

}

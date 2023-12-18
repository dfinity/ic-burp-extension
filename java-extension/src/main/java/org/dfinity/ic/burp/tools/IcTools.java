package org.dfinity.ic.burp.tools;

import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestInfo;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;

// TODO: delete me
public interface IcTools {
    /**
     * Tries to automatically discover the canister CANDID interface by making calls to the IC.
     *
     * @param canisterId The ID of the canister whose interface should be determined
     * @return the CANDID interface specification if discovery was successful, otherwise an empty optional
     * @throws IcToolsException if an error occurs during lookup
     */
    Optional<String> discoverCanisterInterface(String canisterId) throws IcToolsException;

    /**
     * Determines the metadata of a CBOR request body to the IC HTTP API.
     *
     * @param encodedCborRequest the CBOR body that was sent to the IC HTTP API
     * @return metadata about the request
     * @throws IcToolsException if metadata could not be determined
     */
    RequestMetadata getRequestMetadata(byte[] encodedCborRequest) throws IcToolsException;

    /**
     * Decodes a CBOR request body to the IC HTTP API, including the eventually embedded CANDID payload.
     *
     * @param encodedCborRequest the CBOR body that was sent to the IC HTTP API
     * @param canisterInterface  the canister CANDID interface (contents of the DID file), if known
     * @return information about the request, including the CBOR body decoded to JSON and the decoded CANDID payload if any
     * @throws IcToolsException if decoding fails
     */
    RequestInfo decodeCanisterRequest(byte[] encodedCborRequest, Optional<String> canisterInterface) throws IcToolsException;

    /**
     * Decodes a CBOR response body received from the IC HTTP API, including the eventually embedded CANDID payload.
     *
     * @param encodedCborResponse   the CBOR body that was received from the IC HTTP API
     * @param canisterInterfaceInfo the canister CANDID interface and the canister method called, if known
     * @return the CBOR body decoded to JSON and the decoded CANDID payload if any
     * @throws IcToolsException if decoding fails
     */
    String decodeCanisterResponse(byte[] encodedCborResponse, Optional<CanisterInterfaceInfo> canisterInterfaceInfo) throws IcToolsException;
}

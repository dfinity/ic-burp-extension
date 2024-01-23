package org.dfinity.ic.burp.tools;

import org.dfinity.ic.burp.tools.model.CanisterInterfaceInfo;
import org.dfinity.ic.burp.tools.model.DelegationInfo;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.Identity;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestInfo;
import org.dfinity.ic.burp.tools.model.RequestMetadata;

import java.util.Optional;


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

    /**
     * Generates an Ed25519 private key and returns it in PEM format.
     * This key can be used with {@link Identity#ed25519Identity(String)}.
     *
     * @return the generated PKCS#8 v2 PEM-encoded Ed25519 private key
     * @throws IcToolsException if key generation fails
     */
    String generateEd25519Key() throws IcToolsException;

    /**
     * CBOR-encodes a request body and signs it using the provided identity.
     *
     * @param decodedRequest    the decoded request, as returned by {@link #decodeCanisterRequest(byte[], Optional)}
     * @param canisterInterface the canister CANDID interface (contents of the DID file), if known, if this is omitted CANDID types are guessed and the IC might reject the crafted message
     * @param signIdentity      the identity that should be used to sign the request
     * @return the CBOR encoded and signed request
     * @throws IcToolsException if an error occurs during encoding or signing
     */
    byte[] encodeAndSignCanisterRequest(String decodedRequest, Optional<String> canisterInterface, Identity signIdentity) throws IcToolsException;

    /**
     * Adds the provided identity as tentative passkey to the given anchor.
     * This only succeeds if the anchor owner has previously clicked on "Add new passkey" on the II web UI.
     * On success, returns a code that must be submitted by the user via II web UI.
     * Use {@link #internetIdentityIsPasskeyRegistered(String, Identity)} to check if registration of the added passkey was successful.
     *
     * @param anchor       the anchor to which the passkey should be added
     * @param signIdentity the identity that should be added as passkey
     * @return the code that the user must enter to confirm passkey registration
     * @throws IcToolsException if adding a tentative passkey is disabled or some other error occurs
     */
    String internetIdentityAddTentativePasskey(String anchor, Identity signIdentity) throws IcToolsException;

    /**
     * Checks if the provided identity is a registered passkey for the given anchor.
     * Can be polled after calling {@link #internetIdentityAddTentativePasskey(String, Identity)} to check if the user has already entered the code.
     *
     * @param anchor       the anchor that should be checked
     * @param signIdentity the identity that should be checked
     * @return true if the identity was successfully registered as a passkey, false otherwise
     * @throws IcToolsException if an error occurs during checking
     */
    boolean internetIdentityIsPasskeyRegistered(String anchor, Identity signIdentity) throws IcToolsException;

    /**
     * Returns the II principal of the given anchor for the given frontend hostname.
     * This only works if the provided identity is registered as a passkey for the given anchor!
     *
     * @param anchor           the anchor for which the principal should be retrieved
     * @param signIdentity     an identity that is registered as passkey for the given anchor
     * @param frontendHostname the hostname of the frontend
     * @return the principal for the given (anchor, frontend) combination
     * @throws IcToolsException if an error occurs during retrieval, e.g., the signIdentity is not registered as passkey for the given anchor
     */
    Principal internetIdentityGetPrincipal(String anchor, Identity signIdentity, String frontendHostname) throws IcToolsException;

    /**
     * Requests issuing a delegation from II for the given anchor and frontend hostname to the provided session key.
     * This only works if the provided identity is registered as a passkey for the given anchor!
     *
     * @param anchor           the anchor for which a delegation should be issued
     * @param signIdentity     an identity that is registered as passkey for the given anchor
     * @param frontendHostname the hostname of the frontend
     * @param sessionIdentity  the identity that is the delegation target, e.g., Identity.ed25519Identity({@link #generateEd25519Key()})
     * @return the delegation info
     * @throws IcToolsException if an error occurs during delegation issuing, e.g., the signIdentity is not registered as passkey for the given anchor
     */
    DelegationInfo internetIdentityGetDelegation(String anchor, Identity signIdentity, String frontendHostname, Identity sessionIdentity) throws IcToolsException;
}

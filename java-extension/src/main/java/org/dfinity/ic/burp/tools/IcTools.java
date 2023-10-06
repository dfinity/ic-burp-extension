package org.dfinity.ic.burp.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;



public interface IcTools {
    class IcToolsException extends Exception {
        public IcToolsException(String message) {
            super(message);
        }

        public IcToolsException(String message, Throwable cause) {
            super(message, cause);
        }

        public String getStackTraceAsString() {
            StringWriter sw = new StringWriter();
            printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    enum IdentityType {
        ANONYMOUS,
        ED25519,
        SECP256K1;
    }

    class Identity {
        public final IdentityType type;
        public final Optional<String> pem;

        private Identity(IdentityType type, Optional<String> pem) {
            this.type = type;
            this.pem = pem;
        }

        public static Identity anonymousIdentity() {
            // Anonymous identity means no signature is created
            return new Identity(IdentityType.ANONYMOUS, Optional.empty());
        }

        public static Identity ed25519Identity(String pem) {
            // PEM file must contain the PKCS#8 v2 encoded Ed25519 private key
            return new Identity(IdentityType.ED25519, Optional.of(pem));
        }

        public static Identity secp256k1Identity(String pem) {
            // PEM file must contain the SEC1 ASN.1 DER encoded ECPrivateKey
            return new Identity(IdentityType.SECP256K1, Optional.of(pem));
        }
    }

    /**
     * Tries to automatically discover the canister CANDID interface by making calls to the IC.
     *
     * @param canisterId The ID of the canister whose interface should be determined
     * @return the CANDID interface specification if discovery was successful, otherwise an empty optional
     */
    Optional<String> discoverCanisterInterface(String canisterId);


    enum RequestType {
        CALL,
        READ_STATE,
        QUERY;

        public static RequestType from(String type) throws IcToolsException {
            try {
                return RequestType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IcToolsException(String.format("Could not convert %s to request type", type), e);
            }
        }
    }


    class RequestInfo {
        public final RequestType type;
        public final String requestId;
        public final String decodedRequest;
        public final Optional<String> canisterMethod; // not available for /read_state endpoints

        public RequestInfo(RequestType type, String requestId, String decodedRequest, Optional<String> canisterMethod) {
            this.type = type;
            this.requestId = requestId;
            this.decodedRequest = decodedRequest;
            this.canisterMethod = canisterMethod;
        }
    }

    /**
     * Decodes a CBOR request body to the IC HTTP API, including the eventually embedded CANDID payload.
     *
     * @param encodedCborRequest the CBOR body that was sent to the IC HTTP API
     * @param canisterInterface the canister CANDID interface (contents of the DID file), if known
     * @return information about the request, including the CBOR body decoded to JSON and the decoded CANDID payload if any
     * @throws IcToolsException if decoding fails
     */
    RequestInfo decodeCanisterRequest(byte[] encodedCborRequest, Optional<String> canisterInterface) throws IcToolsException;

    class CanisterInterfaceInfo {
        public String canisterInterface;
        public String canisterMethod;
    }

    /**
     * Decodes a CBOR response body received from the IC HTTP API, including the eventually embedded CANDID payload.
     *
     * @param encodedCborResponse the CBOR body that was received from the IC HTTP API
     * @param canisterInterfaceInfo the canister CANDID interface and the canister method called, if known
     * @return the CBOR body decoded to JSON and the decoded CANDID payload if any
     * @throws IcToolsException if decoding fails
     */
    String decodeCanisterResponse(byte[] encodedCborResponse, Optional<CanisterInterfaceInfo> canisterInterfaceInfo) throws IcToolsException;
}

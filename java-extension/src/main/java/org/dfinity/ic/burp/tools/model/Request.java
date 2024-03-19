package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

/**
 * @param requestId      not available for /read_state endpoints that don't query the /request_status path
 * @param canisterId     not available for /read_state endpoints
 * @param canisterMethod not available for /read_state endpoints
 */
public record Request(RequestType type, RequestSenderInfo senderInfo,
                      Optional<String> requestId, Optional<String> canisterId, Optional<String> canisterMethod,
                      byte[] encodedBody, String decodedBody) implements RequestEncoded, RequestDecoded {

    public static RequestMetadata metadata(RequestType type, RequestSenderInfo senderInfo,
                                           Optional<String> requestId, Optional<String> canisterId, Optional<String> canisterMethod) {
        return new Request(type, senderInfo, requestId, canisterId, canisterMethod, null, null);
    }

    public static RequestEncoded encoded(RequestType type, RequestSenderInfo senderInfo,
                                         Optional<String> requestId, Optional<String> canisterId, Optional<String> canisterMethod,
                                         byte[] encodedBody) {
        return new Request(type, senderInfo, requestId, canisterId, canisterMethod, encodedBody, null);
    }

    public static RequestDecoded decoded(RequestType type, RequestSenderInfo senderInfo,
                                         Optional<String> requestId, Optional<String> canisterId, Optional<String> canisterMethod,
                                         String decodedBody) {
        return new Request(type, senderInfo, requestId, canisterId, canisterMethod, null, decodedBody);
    }
}

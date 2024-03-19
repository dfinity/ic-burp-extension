package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

public interface RequestMetadata {
    RequestType type();

    RequestSenderInfo senderInfo();

    /**
     * not available for /read_state endpoints that don't query the /request_status path
     */
    Optional<String> requestId();

    /**
     * not available for /read_state endpoints
     */
    Optional<String> canisterId();

    /**
     * not available for /read_state endpoints
     */
    Optional<String> canisterMethod();
}

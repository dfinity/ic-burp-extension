package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

/**
 * @param requestId      not available for /read_state endpoints that don't query the /request_status path
 * @param canisterMethod not available for /read_state endpoints
 */
public record RequestMetadata(RequestType type, Optional<String> requestId, RequestSenderInfo senderInfo,
                              Optional<String> canisterMethod) {
}

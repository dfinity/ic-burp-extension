package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

/**
 * @param canisterMethod not available for /read_state endpoints
 */
public record RequestMetadata(RequestType type, String requestId, Optional<String> canisterMethod) {
}

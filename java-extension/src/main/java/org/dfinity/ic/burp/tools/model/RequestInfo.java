package org.dfinity.ic.burp.tools.model;

import java.util.Optional;

public class RequestInfo {
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

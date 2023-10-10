package org.dfinity.ic.burp.tools.model;

public enum RequestType {
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

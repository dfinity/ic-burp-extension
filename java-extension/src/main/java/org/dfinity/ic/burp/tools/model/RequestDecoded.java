package org.dfinity.ic.burp.tools.model;

public interface RequestDecoded extends RequestMetadata {
    String decodedBody();
}

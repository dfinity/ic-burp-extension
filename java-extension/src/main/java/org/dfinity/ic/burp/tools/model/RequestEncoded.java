package org.dfinity.ic.burp.tools.model;

public interface RequestEncoded extends RequestMetadata {
    byte[] encodedBody();
}

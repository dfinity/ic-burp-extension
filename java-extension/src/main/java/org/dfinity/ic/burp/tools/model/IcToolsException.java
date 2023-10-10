package org.dfinity.ic.burp.tools.model;

import java.io.PrintWriter;
import java.io.StringWriter;

public class IcToolsException extends Exception {
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

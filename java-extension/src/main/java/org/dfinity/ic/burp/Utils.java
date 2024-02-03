package org.dfinity.ic.burp;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
    public static String getStacktrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}

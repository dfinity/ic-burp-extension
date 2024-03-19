package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.Optional;
import java.util.regex.Pattern;

public record UrlPathInfo(String canisterId, RequestType requestType) {
    private static final Pattern IC_API_PATH_REGEX = Pattern.compile("/api/v2/canister/(?<cid>[^/]+)/(?<rtype>query|call|read_state)");

    public static Optional<UrlPathInfo> tryFrom(String path) {
        var matcher = IC_API_PATH_REGEX.matcher(path);
        if (matcher.matches() && matcher.group("cid") != null && matcher.group("rtype") != null) {
            try {
                return Optional.of(new UrlPathInfo(matcher.group("cid"), RequestType.valueOf(matcher.group("rtype").toUpperCase())));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }
}

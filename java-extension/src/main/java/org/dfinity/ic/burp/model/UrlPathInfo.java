package org.dfinity.ic.burp.model;

import org.dfinity.ic.burp.tools.model.RequestType;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record UrlPathInfo(String canisterId, RequestType requestType) {
    private static final Pattern IC_API_V2_PATH_REGEX = Pattern.compile("/api/v2/canister/(?<cid>[^/]+)/(?<rtype>query|call|read_state)");
    private static final Pattern IC_API_V3_PATH_REGEX = Pattern.compile("/api/v3/canister/(?<cid>[^/]+)/(?<rtype>call)");

    public static Optional<UrlPathInfo> tryFrom(String path) {
        for (var pathRegex : List.of(IC_API_V2_PATH_REGEX, IC_API_V3_PATH_REGEX)) {
            var matcher = pathRegex.matcher(path);
            if (matcher.matches() && matcher.group("cid") != null && matcher.group("rtype") != null) {
                try {
                    return Optional.of(new UrlPathInfo(matcher.group("cid"), RequestType.valueOf(matcher.group("rtype").toUpperCase())));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return Optional.empty();
    }
}

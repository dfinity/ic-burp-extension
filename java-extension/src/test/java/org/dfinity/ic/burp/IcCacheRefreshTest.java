package org.dfinity.ic.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.logging.Logging;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;
import org.dfinity.ic.burp.tools.model.Principal;
import org.dfinity.ic.burp.tools.model.RequestMetadata;
import org.dfinity.ic.burp.tools.model.RequestSenderInfo;
import org.dfinity.ic.burp.tools.model.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IcCacheRefreshTest {

    private static final RequestMetadata META_DATA = new RequestMetadata(RequestType.CALL, Optional.of("request_id"), new RequestSenderInfo(Principal.anonymous(), Optional.empty(), Optional.empty(), List.of()), Optional.of("canister_method"));
    private final byte[] BODY_BYTES = new byte[]{0, 1, 2, 3};
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache = Caffeine.newBuilder().buildAsync(
            a -> new CanisterCacheInfo(a, InterfaceType.AUTOMATIC));
    private final Cache<String, RequestMetadata> callRequestCache = Caffeine.newBuilder().build();

    @Mock
    private HttpRequestToBeSent request;
    @Mock
    private HttpResponseReceived response;
    @Mock
    private IcTools tools;
    @Mock
    private RequestToBeSentAction requestAction;
    @Mock
    private ResponseReceivedAction responseAction;
    private IcCacheRefresh cacheRefresh;

    private void returnHttpHeader(Optional<String> contentType, Optional<String> contentLength) {

        HttpHeader contentTypeHeader;
        if (contentType.isPresent()) {
            contentTypeHeader = mock(HttpHeader.class);
            when(contentTypeHeader.name()).thenReturn("Content-Type");
            when(contentTypeHeader.value()).thenReturn(contentType.get());
        } else {
            contentTypeHeader = null;
        }
        HttpHeader contentLengthHeader;
        if (contentLength.isPresent()) {
            contentLengthHeader = mock(HttpHeader.class);
            when(contentLengthHeader.name()).thenReturn("Content-Length");
            when(contentLengthHeader.value()).thenReturn(contentLength.get());
        } else {
            contentLengthHeader = null;
        }

        when(request.header("Content-Type")).thenReturn(contentTypeHeader);
        when(request.header("Content-Length")).thenReturn(contentLengthHeader);
    }

    @BeforeEach
    void init() throws IcToolsException {
        ByteArray body = mock(ByteArray.class);
        when(body.getBytes()).thenReturn(BODY_BYTES);
        when(request.body()).thenReturn(body);

        when(tools.getRequestMetadata(BODY_BYTES)).thenReturn(META_DATA);

        var log = mock(Logging.class);
        cacheRefresh = new IcCacheRefresh(log, tools, canisterInterfaceCache, callRequestCache, Optional.of(x -> x == request ? requestAction : null), Optional.of(x -> x == response ? responseAction : null));
    }

    @Test
    public void shouldNotCacheForNotMatchingUrl() {
        when(request.path()).thenReturn("/not/matching/path");

        var res = cacheRefresh.handleHttpRequestToBeSent(request);

        assertEquals(requestAction, res);
        assertEquals(0, canisterInterfaceCache.asMap().size());
        assertEquals(0, callRequestCache.asMap().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v2/canister/4c4fd-caaaa-aaaaq-aaa3a-cai/query",
            "/api/v2/canister/4c4fd-caaaa-aaaaq-aaa3a-cai/call",
            "/api/v2/canister/4c4fd-caaaa-aaaaq-aaa3a-cai/read_state"})
    public void shouldCacheInterface(String path) {
        when(request.path()).thenReturn(path);

        var res = cacheRefresh.handleHttpRequestToBeSent(request);

        assertEquals(requestAction, res);
        assertEquals(new CanisterCacheInfo("4c4fd-caaaa-aaaaq-aaa3a-cai", InterfaceType.AUTOMATIC).getCanisterInterfaces()
                , canisterInterfaceCache.get("4c4fd-caaaa-aaaaq-aaa3a-cai").join().getCanisterInterfaces());
    }

    @Test
    public void shouldCacheMetadataOfCall() {
        when(request.path()).thenReturn("/api/v2/canister/4c4fd-caaaa-aaaaq-aaa3a-cai/call");
        returnHttpHeader(Optional.of("application/cbor"), Optional.of("1"));

        var res = cacheRefresh.handleHttpRequestToBeSent(request);

        assertEquals(requestAction, res);
        assertEquals(META_DATA, callRequestCache.getIfPresent(META_DATA.requestId().orElseThrow()));
    }

    @Test
    public void shouldContinueIfRequestIsMalformed() {
        when(request.path()).thenThrow(new MalformedRequestException("request malformed"));

        var res = cacheRefresh.handleHttpRequestToBeSent(request);

        assertEquals(requestAction, res);
        assertEquals(0, canisterInterfaceCache.asMap().size());
        assertEquals(0, callRequestCache.asMap().size());
    }

    @Test
    public void shouldContinueIfRequestTypeIsInvalid() {
        when(request.path()).thenReturn("/api/v2/canister/4c4fd-caaaa-aaaaq-aaa3a-cai/invalid_request_type");

        var res = cacheRefresh.handleHttpRequestToBeSent(request);

        assertEquals(requestAction, res);
        assertEquals(0, canisterInterfaceCache.asMap().size());
        assertEquals(0, callRequestCache.asMap().size());
    }

    @Test
    public void shouldForwardResponse() {
        var res = cacheRefresh.handleHttpResponseReceived(response);

        assertEquals(responseAction, res);
    }
}
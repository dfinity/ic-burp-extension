package org.dfinity.ic.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.MalformedRequestException;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.ui.editor.RawEditor;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestInfo;
import org.dfinity.ic.burp.tools.model.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IcHttpRequestResponseViewerTest {

    private final byte[] BODY_BYTES = new byte[]{0, 1, 2, 3};
    @Mock
    HttpRequestResponse requestResponse;
    @Mock
    HttpRequest request;
    @Mock
    HttpResponse response;
    @Mock
    private MontoyaApi api;
    @Mock
    private RawEditor rawEditor;
    @Mock
    private IcTools tools;

    private void returnHttpHeader(boolean forRequest, Optional<String> contentType, Optional<String> contentLength) {

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

        if (forRequest) {
            when(request.header("Content-Type")).thenReturn(contentTypeHeader);
            when(request.header("Content-Length")).thenReturn(contentLengthHeader);
        } else {
            when(response.header("Content-Type")).thenReturn(contentTypeHeader);
            when(response.header("Content-Length")).thenReturn(contentLengthHeader);
        }
    }

    private ByteArray mockByteArrayFactory(String s) {
        ByteArray array = mock(ByteArray.class);
        when(array.toString()).thenReturn(s);
        return array;
    }

    @BeforeEach
    void init() {
        UserInterface ui = mock(UserInterface.class);
        when(ui.createRawEditor(any())).thenReturn(rawEditor);
        when(api.userInterface()).thenReturn(ui);

        when(requestResponse.request()).thenReturn(request);
        when(requestResponse.response()).thenReturn(response);

        when(request.path()).thenReturn("/api/v2/canister/vtrom-gqaaa-aaaaq-aabia-cai/query");

        ByteArray body = mock(ByteArray.class);
        when(body.getBytes()).thenReturn(BODY_BYTES);
        when(request.body()).thenReturn(body);
        when(response.body()).thenReturn(body);
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v2/canister/vtrom-gqaaa-aaaaq-aabia-cai/query,true",
            "/api/v2/canister/rrkah-fqaaa-aaaaa-aaaaq-cai/call,true",
            "/api/v2/canister/rrkah-fqaaa-aaaaa-aaaaq-cai/read_state,true",
            "/api/v2/canister/vtrom-gqaaa-aaaaq-aabia-cai/query,false",
            "/api/v2/canister/rrkah-fqaaa-aaaaa-aaaaq-cai/call,false",
            "/api/v2/canister/rrkah-fqaaa-aaaaa-aaaaq-cai/read_state,false"})
    public void shouldBeEnabledForIcRequestsAndResponses(String path, boolean isRequest) {
        returnHttpHeader(isRequest, Optional.of("application/cbor"), Optional.of("123"));
        when(request.path()).thenReturn(path);

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertTrue(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldBeDisabledIfPathIsMalformed(boolean isRequest) {
        when(request.path()).thenThrow(new MalformedRequestException("malformed"));

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertFalse(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldBeDisabledIfContentTypeIsNotPresent(boolean isRequest) {
        returnHttpHeader(isRequest, Optional.empty(), Optional.of("123"));

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertFalse(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldBeDisabledIfContentTypeIsWrong(boolean isRequest) {
        returnHttpHeader(isRequest, Optional.of("application/json"), Optional.of("123"));

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertFalse(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldBeDisabledIfContentLengthIsNotPresent(boolean isRequest) {
        returnHttpHeader(isRequest, Optional.of("application/cbor"), Optional.empty());

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertFalse(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldBeDisabledIfContentLengthIsWrong(boolean isRequest) {
        returnHttpHeader(isRequest, Optional.of("application/cbor"), Optional.of("0"));

        var res = new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.empty()).isEnabledFor(requestResponse);

        assertFalse(res);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldDecodeBody(boolean isRequest) throws IcToolsException {
        if (isRequest) {
            when(tools.decodeCanisterRequest(BODY_BYTES, Optional.empty())).thenReturn(new RequestInfo(RequestType.QUERY, "123", "decodedBody", Optional.empty()));
        } else {
            when(tools.decodeCanisterResponse(BODY_BYTES, Optional.empty())).thenReturn("decodedBody");
        }

        new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.of(this::mockByteArrayFactory)).setRequestResponse(requestResponse);

        ArgumentCaptor<ByteArray> captor = ArgumentCaptor.forClass(ByteArray.class);
        verify(rawEditor).setContents(captor.capture());
        assertEquals("decodedBody", captor.getValue().toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldFailToDecodeBody(boolean isRequest) throws IcToolsException {
        if (isRequest) {
            when(tools.decodeCanisterRequest(BODY_BYTES, Optional.empty())).thenThrow(new IcToolsException("something went wrong"));
        } else {
            when(tools.decodeCanisterResponse(BODY_BYTES, Optional.empty())).thenThrow(new IcToolsException("something went wrong"));
        }

        new IcHttpRequestResponseViewer(api, tools, isRequest, Optional.of(this::mockByteArrayFactory)).setRequestResponse(requestResponse);

        ArgumentCaptor<ByteArray> captor = ArgumentCaptor.forClass(ByteArray.class);
        verify(rawEditor).setContents(captor.capture());
        assertTrue(captor.getValue().toString().startsWith("Failed to decode " + (isRequest ? "request" : "response")));
    }
}
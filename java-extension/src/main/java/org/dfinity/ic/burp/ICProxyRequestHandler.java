package org.dfinity.ic.burp;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;

/*
TODO This class aimed to test exactly what type of request manipulation was possible and where in Burp it
would show up. It seems that we can't alter the proxy history this way and thus, sending requests to other tools such
as the repeater, intruder, etc from the proxy history would always operate on the original raw request.

This Handler can probably be removed in the MVP.
 */
public class ICProxyRequestHandler implements ProxyRequestHandler {

    private final Logging log;

    public ICProxyRequestHandler(Logging log) {
        this.log = log;
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        log.logToOutput("ICProxyRequestHandler.handleRequestReceived: " + interceptedRequest.url());
        return ProxyRequestReceivedAction.continueWith(interceptedRequest.withBody("TESTING"));
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        log.logToOutput("ICProxyRequestHandler.handleRequestToBeSent: " + interceptedRequest.url());
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest.withBody("TESTING2"));
    }
}

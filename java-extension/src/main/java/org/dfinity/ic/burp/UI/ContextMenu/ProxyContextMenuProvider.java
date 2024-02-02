package org.dfinity.ic.burp.UI.ContextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import org.dfinity.ic.burp.IcHttpRequestResponseViewer;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.RequestInfo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.dfinity.ic.burp.IcBurpExtension.*;


public class ProxyContextMenuProvider implements  ContextMenuItemsProvider{
    private final MontoyaApi api;
    private final IcTools icTools;
    private final AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache;
    private final InternetIdentities internetIdentities;

    public ProxyContextMenuProvider(MontoyaApi api, IcTools icTools, AsyncLoadingCache<String, CanisterCacheInfo> canisterInterfaceCache, InternetIdentities internetIdentities) {
        this.api = api;
        this.icTools = icTools;
        this.canisterInterfaceCache = canisterInterfaceCache;
        this.internetIdentities = internetIdentities;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // Depending on whether the user opened the context menu in the editor or on the proxy history,
        // either of the following two variables will be empty.
        List<HttpRequestResponse> requestResponses = event.selectedRequestResponses();
        Optional<MessageEditorHttpRequestResponse> messageEditorRequestResponse = event.messageEditorRequestResponse();

        // Will contain the sum of the requestResponses from the messageEditor selected requestResponses and the requestResponses
        // selected in the table.
        List<HttpRequestResponse> totalRequestResponses = new ArrayList<>(requestResponses);

        messageEditorRequestResponse.ifPresent(messageEditorHttpRequestResponse -> totalRequestResponses.add(messageEditorHttpRequestResponse.requestResponse()));

        if(totalRequestResponses.isEmpty()){
            // This returns an empty list of menu items.
            return ContextMenuItemsProvider.super.provideMenuItems(event);
        }

        List<HttpRequest> httpRequestList = new ArrayList<>();
        for (HttpRequestResponse requestResponse : totalRequestResponses) {
            HttpRequest req = requestResponse.request();

            // TODO Similar code to IcHttpRequestResponseViewer.enabledFor Maybe deduplicate?
            String path = req.path();
            HttpHeader content_type = req.header("Content-Type");
            if (content_type == null || !content_type.value().equals("application/cbor")) {
                continue;
            }

            Optional<String> cid = IcHttpRequestResponseViewer.getCanisterId(path);

            if (cid.isEmpty()) {
                continue;
            }

            CompletableFuture<CanisterCacheInfo> canisterCacheInfo = canisterInterfaceCache.getIfPresent(cid.get());
            if (canisterCacheInfo == null) {
                continue;
            }

            Optional<String> idl = canisterCacheInfo.join().getActiveCanisterInterface();
            try {
                RequestInfo requestInfo = icTools.decodeCanisterRequest(req.body().getBytes(), idl);
                Optional<List<String>> result = internetIdentities.findAnchor(requestInfo.senderInfo(), req.headerValue("Origin"));
                if(result.isPresent()) {
                    req = req.withAddedHeader(IC_DECODED_HEADER_NAME, "True");
                    req = req.withAddedHeader(IC_SIGN_IDENTITY_HEADER_NAME, result.get().get(0));
                    req = req.withAddedHeader(IC_ORIGIN_HEADER_NAME, result.get().get(1));
                }
                httpRequestList.add(req.withBody(requestInfo.decodedRequest()));

            } catch (IcToolsException e) {
                api.logging().logToError("Unable to request metadata for request with URI: " + req.url(), e);
            }
        }

        if(httpRequestList.isEmpty())
            // This returns an empty list of menu items.
            return ContextMenuItemsProvider.super.provideMenuItems(event);

        JMenuItem menuItemRepeater = new JMenuItem("Send to repeater (IC Decoded)");
        menuItemRepeater.addActionListener(l -> {
            for(HttpRequest r : httpRequestList){
                // This header is added to easily detect which outgoing requests need to be re-encoded and resigned.
                api.repeater().sendToRepeater(r);
            }
        });

        JMenuItem menuItemIntruder = new JMenuItem("Send to intruder (IC Decoded)");
        menuItemIntruder.addActionListener(l -> {
            for(HttpRequest r : httpRequestList){
                // This header is added to easily detect which outgoing requests need to be re-encoded and resigned.
                api.intruder().sendToIntruder(r);
            }
        });

        List<Component> menuItemList = new ArrayList<>();
        menuItemList.add(menuItemRepeater);
        menuItemList.add(menuItemIntruder);
        return menuItemList;
    }
}

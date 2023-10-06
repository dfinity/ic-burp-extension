package org.dfinity.ic.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import org.dfinity.ic.burp.tools.jna.JnaIcTools;

public class IcBurpExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("IC Burp Extension " + getClass().getPackage().getImplementationVersion());

        var icTools = new JnaIcTools();
        var viewerProvider = new IcHttpRequestResponseViewerProvider(api, icTools);
        api.userInterface().registerHttpRequestEditorProvider(viewerProvider);
        api.userInterface().registerHttpResponseEditorProvider(viewerProvider);
    }
}

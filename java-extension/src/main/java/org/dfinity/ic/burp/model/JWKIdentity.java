package org.dfinity.ic.burp.model;

import burp.api.montoya.logging.Logging;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.dfinity.ic.burp.tools.model.Identity;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.ECPrivateKey;
import java.util.UUID;

public class JWKIdentity {
    private final ECKey jwk;
    private final Logging log;

    // Generated a new JWK keypair.
    public JWKIdentity(Logging log) throws JOSEException {
        this.log = log;
        jwk = new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();

        log.logToOutput(jwk.toJSONString()
        );

        ECPrivateKey privKey = jwk.toECPrivateKey();
        log.logToOutput(privKey.getFormat());
        log.logToOutput(getScript());
    }

    public String getScript(){
        String template = "Failed to load script file.";

        try {
            InputStream is = this.getClass().getResourceAsStream("/importScript.js");
            if (is != null) {
                template = new String(is.readAllBytes());
            };
        } catch (IOException e) {
            return "Failed to load script file.";
        };

        template = template.replaceAll("<PLACEHOLDER>", jwk.toJSONString());

        return template;
    }
}

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
import java.util.Base64;
import java.util.UUID;


/*
Atm, agent-rs does not support P_256 (secp256r1) which is used by agent-js. Hence, the identities generated here can't
be used by jnatools. A future update of agent-rs should include support for secp256r1.

Until then, this code is unused.
 */
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

    public Identity getIdentity(){
        String pem;
        try {
            ECPrivateKey priv = jwk.toECPrivateKey();
            pem = "-----BEGIN EC PRIVATE KEY-----\n";
            pem += Base64.getEncoder().encodeToString(priv.getEncoded());
            pem += "\n-----END EC PRIVATE KEY-----";

            // TODO This is not a secp256k1 but a secp256r1 key. There is currently no support for the r1 variant in jnatools (agent-rs).
            // To be replaced once support is implemented.
            return Identity.secp256k1Identity(pem);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String getScript(){
        String template = "Failed to load script file.";

        try {
            InputStream is = this.getClass().getResourceAsStream("/importScript.js");
            if (is != null) {
                template = new String(is.readAllBytes());
            }
        } catch (IOException e) {
            return "Failed to load script file.";
        }

        template = template.replaceAll("<PLACEHOLDER>", jwk.toJSONString());

        return template;
    }
}

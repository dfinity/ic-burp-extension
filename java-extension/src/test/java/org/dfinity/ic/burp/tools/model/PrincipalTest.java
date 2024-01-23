package org.dfinity.ic.burp.tools.model;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrincipalTest {

    @Test
    public void shouldRepresentManagementCanister() {
        var principal = Principal.managementCanister();

        assertEquals("aaaaa-aa", principal.toString());
        assertEquals(0, principal.id().size());
        assertEquals("", principal.getIdAsHexString());
        assertEquals(Principal.fromText("aaaaa-aa"), principal);
    }

    @Test
    public void shouldRepresentAnonymousPrincipal() {
        var principal = Principal.anonymous();

        assertEquals("2vxsx-fae", principal.toString());
        assertEquals(List.of((byte) 4), principal.id());
        assertEquals("04", principal.getIdAsHexString());
        assertEquals(Principal.fromText("2vxsx-fae"), principal);
    }

    @Test
    public void shouldRepresentSelfAuthenticatingPrincipal() {
        var principal = Principal.fromText("77n2f-wxwep-ktsew-dkfnr-kk5ln-higr2-52mry-ikf3b-5uvqi-hjpwp-5qe");

        assertEquals("77n2f-wxwep-ktsew-dkfnr-kk5ln-higr2-52mry-ikf3b-5uvqi-hjpwp-5qe", principal.toString());
        assertEquals("f623d53912c3515b152bab69d068ebba6470851761ed2b041d2fb3fb02", principal.getIdAsHexString());
        assertEquals(Principal.fromBytes(HexFormat.of().parseHex("f623d53912c3515b152bab69d068ebba6470851761ed2b041d2fb3fb02")), principal);
        assertEquals(Principal.selfAuthenticating("MCowBQYDK2VwAyEANFTu7K+ZilTs4wgjwY6+DgKNeUn1JehRBYHqulmSF8c"), principal);
    }
}
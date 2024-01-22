package org.dfinity.ic.burp.tools.jna;

import org.dfinity.ic.burp.tools.model.Principal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JnaDelegationParserTest {

    @Test
    public void shouldParseDelegationWithoutTargets() {
        var delegation_vector = "1;MCowBQYDK2VwAyEAbF9MEgey18w2dlCqr+8NTEbq+3YZbmCgIKTwZh5xdmo:8446744073709551615:0:YA2/Po31+jShsmtXmrcfAOS2SXWcMFxXNWwGB0epZn17oruUkjQckRbW3n8cOWrMzbKv3Fbcy+rL/x0AfZfJBQ";

        var delegation = JnaDelegationParser.parseDelegations(delegation_vector);

        assertEquals(1, delegation.size());
        assertEquals("MCowBQYDK2VwAyEAbF9MEgey18w2dlCqr+8NTEbq+3YZbmCgIKTwZh5xdmo", delegation.get(0).pubkey());
        assertEquals(8446744073709551615L, delegation.get(0).expiration());
        assertEquals(0, delegation.get(0).targets().size());
        assertEquals("YA2/Po31+jShsmtXmrcfAOS2SXWcMFxXNWwGB0epZn17oruUkjQckRbW3n8cOWrMzbKv3Fbcy+rL/x0AfZfJBQ", delegation.get(0).signature());
    }

    @Test
    public void shouldParseDelegationWithTargets() {
        var delegation_vector = "2;MCowBQYDK2VwAyEArRhC/z6LVM2QKC+F77x2x7zNNhfgvtcsLOQfj57FWGM:8446744073709551615:2,aaaaa-aa,2vxsx-fae:TbujfeosClZe3Ex8yOtj8FACdB9vxJi/WIyuDS8rfAPh9XGbff0C3zMgkagLLeL7xbTY2OHZYaLrQh34vUH/CQ;MCowBQYDK2VwAyEAbF9MEgey18w2dlCqr+8NTEbq+3YZbmCgIKTwZh5xdmo:8446744073709551615:0:YA2/Po31+jShsmtXmrcfAOS2SXWcMFxXNWwGB0epZn17oruUkjQckRbW3n8cOWrMzbKv3Fbcy+rL/x0AfZfJBQ";

        var delegation = JnaDelegationParser.parseDelegations(delegation_vector);

        assertEquals(2, delegation.size());

        assertEquals("MCowBQYDK2VwAyEArRhC/z6LVM2QKC+F77x2x7zNNhfgvtcsLOQfj57FWGM", delegation.get(0).pubkey());
        assertEquals(8446744073709551615L, delegation.get(0).expiration());
        assertEquals(2, delegation.get(0).targets().size());
        assertEquals(Principal.managementCanister(), delegation.get(0).targets().get(0));
        assertEquals(Principal.anonymous(), delegation.get(0).targets().get(1));
        assertEquals("TbujfeosClZe3Ex8yOtj8FACdB9vxJi/WIyuDS8rfAPh9XGbff0C3zMgkagLLeL7xbTY2OHZYaLrQh34vUH/CQ", delegation.get(0).signature());

        assertEquals("MCowBQYDK2VwAyEAbF9MEgey18w2dlCqr+8NTEbq+3YZbmCgIKTwZh5xdmo", delegation.get(1).pubkey());
        assertEquals(8446744073709551615L, delegation.get(1).expiration());
        assertEquals(0, delegation.get(1).targets().size());
        assertEquals("YA2/Po31+jShsmtXmrcfAOS2SXWcMFxXNWwGB0epZn17oruUkjQckRbW3n8cOWrMzbKv3Fbcy+rL/x0AfZfJBQ", delegation.get(1).signature());
    }
}

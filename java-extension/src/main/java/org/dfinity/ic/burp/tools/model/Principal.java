package org.dfinity.ic.burp.tools.model;

import org.apache.commons.codec.binary.Base32;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32;

public record Principal(List<Byte> id) {
    private static final int MAX_LENGTH_IN_BYTES = 29;

    /**
     * Creates a new principal with the given raw ID bytes
     *
     * @param id raw bytes of the principal
     */
    public Principal(List<Byte> id) {
        if (id.size() > MAX_LENGTH_IN_BYTES)
            throw new RuntimeException("Invalid principal: max length = " + MAX_LENGTH_IN_BYTES + ", actual length = " + id.size());
        // we want an immutable list
        this.id = id.stream().toList();
    }

    /**
     * Constructs the management canister principal aaaaa-aa
     *
     * @return the constructed principal
     */
    public static Principal managementCanister() {
        return fromBytes(new byte[0]);
    }

    /**
     * Constructs the anonymous principal 2vxsx-fae
     *
     * @return the constructed principal
     */
    public static Principal anonymous() {
        return fromBytes(new byte[]{4});
    }

    /**
     * Constructs a self authenticating principal from a base64 encoded public key
     *
     * @param pubkey base64 encoded public key
     * @return the constructed principal
     */
    public static Principal selfAuthenticating(String pubkey) {
        byte[] keyBytes = Base64.getDecoder().decode(pubkey);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-224");
            byte[] hash = md.digest(keyBytes);
            byte[] hashAndTag = Arrays.copyOf(hash, hash.length + 1);
            hashAndTag[hash.length] = 2;
            return fromBytes(hashAndTag);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs a principal from the given raw ID bytes
     *
     * @param id raw bytes of the principal <a href="https://internetcomputer.org/docs/current/references/ic-interface-spec#id-classes">see spec</a>
     * @return the constructed principal
     */
    public static Principal fromBytes(byte[] id) {
        var list = new ArrayList<Byte>();
        for (byte b : id) list.add(b);
        return new Principal(list);
    }

    /**
     * Convenience wrapper method for {@link #fromBytes(byte[])} which expects raw ID bytes as hex string
     *
     * @param hexBytes raw ID bytes as hex string
     * @return the constructed principal
     */
    public static Principal fromHexBytes(String hexBytes) {
        return fromBytes(HexFormat.of().parseHex(hexBytes));
    }

    /**
     * Constructs a principal from a textual representation
     *
     * @param text the textual representation of a principal <a href="https://internetcomputer.org/docs/current/references/ic-interface-spec#textual-ids">see spec</a>
     * @return the constructed principal
     */
    public static Principal fromText(String text) {
        text = text.replace("-", "").toLowerCase();

        var bytes = new Base32().decode(text);
        if (bytes.length < 4)
            throw new RuntimeException("Text is too short.");

        var buf = ByteBuffer.wrap(bytes);
        // we need to prevent sign extension when converting to long
        long checksum = buf.slice(0, 4).getInt() & 0x00000000ffffffffL;
        var data = buf.slice(4, bytes.length - 4);

        CRC32 crc = new CRC32();
        crc.update(data);
        if (crc.getValue() != checksum) {
            throw new RuntimeException("CRC32 check sequence doesn't match with calculated from Principal bytes.");
        }

        // crc.update() has consumed the buffer, so we need to flip it
        data.flip();
        var dataBytes = new byte[data.remaining()];
        data.get(dataBytes);
        return fromBytes(dataBytes);
    }

    /**
     * Returns the raw ID hex encoded as string
     *
     * @return raw ID hex encoded of the principal
     */
    public String getIdAsHexString() {
        var bytes = new byte[id.size()];
        int ptr = 0;
        for (byte b : id) bytes[ptr++] = b;
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * @return textual representation of the principal
     */
    @Override
    public String toString() {
        var bytes = new byte[4 + id.size()];
        int ptr = 4;
        for (byte b : id) bytes[ptr++] = b;
        var buf = ByteBuffer.wrap(bytes);
        CRC32 crc = new CRC32();
        crc.update(buf.slice(4, id.size()));
        buf.putInt((int) crc.getValue());

        var enc = new Base32().encodeAsString(bytes).toLowerCase();
        var sb = new StringBuilder();
        for (int i = 0; i < enc.length(); i++) {
            if (enc.charAt(i) == '=')
                break;
            if (i > 0 && i % 5 == 0)
                sb.append("-");
            sb.append(enc.charAt(i));
        }
        return sb.toString();
    }
}

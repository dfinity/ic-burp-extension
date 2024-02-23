package org.dfinity.ic.burp.storage;

import burp.api.montoya.persistence.Preferences;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HierarchicPreferencesTest {

    private final Preferences preferences = new TestPreferences();

    @Test
    public void shouldStoreAndLoadFlatPreferences() {
        var in = new HierarchicPreferences();
        in.setBoolean("boolA", true);
        in.setBoolean("boolB", true);
        in.setByte("byteA", (byte) 0);
        in.setInteger("integerA", 123);
        in.setInteger("integerB", 456);
        in.setInteger("integerC", 789);
        in.setShort("shortA", (short) -1);
        in.setLong("longA", 123456789);
        in.setString("stringA", "foo");
        in.setString("stringB", "bar");
        in.setString("stringC", "baz");

        in.store(preferences, "PREF");

        var out = HierarchicPreferences.from(preferences, "PREF").orElseThrow();

        assertEquals(in, out);
    }

    @Test
    public void shouldLoadAndStoreNestedPreferences() {
        var in = new HierarchicPreferences();
        var nested = new HierarchicPreferences();
        nested.setBoolean("boolA", true);
        nested.setBoolean("boolB", true);
        nested.setByte("byteA", (byte) 0);
        nested.setInteger("integerA", 123);
        nested.setInteger("integerB", 456);
        nested.setInteger("integerC", 789);
        nested.setShort("shortA", (short) -1);
        nested.setLong("longA", 123456789);
        nested.setString("stringA", "foo");
        nested.setString("stringB", "bar");
        nested.setString("stringC", "baz");
        in.setChildObject("nested", nested);

        in.store(preferences, "pref");

        var out = HierarchicPreferences.from(preferences, "pref").orElseThrow();

        assertEquals(in, out);
        assertEquals(nested, out.getChildObject("nested"));
    }

    @Test
    public void shouldThrowWhenStoringEmptyObject() {
        assertThrows(RuntimeException.class, () -> new HierarchicPreferences().store(preferences, "foo"));
    }

    @Test
    public void shouldReturnEmptyOptionalIfKeyDoesNotExist() {
        assertEquals(Optional.empty(), HierarchicPreferences.from(preferences, "foo"));
    }

    @Test
    public void shouldThrowIfKeyOrValueContainsIllegalChar() {
        var hp = new HierarchicPreferences();
        assertThrows(RuntimeException.class, () -> hp.setChildObject("a$", new HierarchicPreferences()));
        assertThrows(RuntimeException.class, () -> hp.setByte("a#", (byte) 1));
        assertThrows(RuntimeException.class, () -> hp.setBoolean("a#aa", true));
        assertThrows(RuntimeException.class, () -> hp.setLong("$", 123));
        assertThrows(RuntimeException.class, () -> hp.setInteger("#", 123));
        assertThrows(RuntimeException.class, () -> hp.setShort("#foo", (short) 123));
        assertThrows(RuntimeException.class, () -> hp.setString("$foo", "a"));
        assertThrows(RuntimeException.class, () -> hp.setString("foo", "#a"));
    }

    private static class TestPreferences implements Preferences {
        private final Map<String, Boolean> booleans = new HashMap<>();
        private final Map<String, Byte> bytes = new HashMap<>();
        private final Map<String, Integer> integers = new HashMap<>();
        private final Map<String, Long> longs = new HashMap<>();
        private final Map<String, Short> shorts = new HashMap<>();
        private final Map<String, String> strings = new HashMap<>();

        @Override
        public String getString(String key) {
            return strings.get(key);
        }

        @Override
        public void setString(String key, String value) {
            strings.put(key, value);
        }

        @Override
        public void deleteString(String key) {
            strings.remove(key);
        }

        @Override
        public Set<String> stringKeys() {
            return strings.keySet();
        }

        @Override
        public Boolean getBoolean(String key) {
            return booleans.get(key);
        }

        @Override
        public void setBoolean(String key, boolean value) {
            booleans.put(key, value);
        }

        @Override
        public void deleteBoolean(String key) {
            booleans.remove(key);
        }

        @Override
        public Set<String> booleanKeys() {
            return booleans.keySet();
        }

        @Override
        public Byte getByte(String key) {
            return bytes.get(key);
        }

        @Override
        public void setByte(String key, byte value) {
            bytes.put(key, value);
        }

        @Override
        public void deleteByte(String key) {
            bytes.remove(key);
        }

        @Override
        public Set<String> byteKeys() {
            return bytes.keySet();
        }

        @Override
        public Short getShort(String key) {
            return shorts.get(key);
        }

        @Override
        public void setShort(String key, short value) {
            shorts.put(key, value);
        }

        @Override
        public void deleteShort(String key) {
            shorts.remove(key);
        }

        @Override
        public Set<String> shortKeys() {
            return shorts.keySet();
        }

        @Override
        public Integer getInteger(String key) {
            return integers.get(key);
        }

        @Override
        public void setInteger(String key, int value) {
            integers.put(key, value);
        }

        @Override
        public void deleteInteger(String key) {
            integers.remove(key);
        }

        @Override
        public Set<String> integerKeys() {
            return integers.keySet();
        }

        @Override
        public Long getLong(String key) {
            return longs.get(key);
        }

        @Override
        public void setLong(String key, long value) {
            longs.put(key, value);
        }

        @Override
        public void deleteLong(String key) {
            longs.remove(key);
        }

        @Override
        public Set<String> longKeys() {
            return longs.keySet();
        }
    }
}
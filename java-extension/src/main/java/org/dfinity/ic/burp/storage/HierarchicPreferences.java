package org.dfinity.ic.burp.storage;

import burp.api.montoya.persistence.Preferences;
import org.dfinity.ic.burp.model.PreferenceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HierarchicPreferences implements Preferences {
    public static final String KEY_SEPARATOR = "#";
    public static final String TYPE_VALUE_SEPARATOR = "$";
    public static final String RESERVED_CHARS = KEY_SEPARATOR + TYPE_VALUE_SEPARATOR;
    public static final String BOOLEAN_TYPE = "Boolean";
    public static final String BYTE_TYPE = "Byte";
    public static final String INTEGER_TYPE = "Integer";
    public static final String LONG_TYPE = "Long";
    public static final String SHORT_TYPE = "Short";
    public static final String STRING_TYPE = "String";
    public static final String CHILD_TYPE = "Child";
    private final Map<String, Integer> integers = new HashMap<>();
    private final Map<String, Boolean> booleans = new HashMap<>();
    private final Map<String, Byte> bytes = new HashMap<>();
    private final Map<String, Long> longs = new HashMap<>();
    private final Map<String, Short> shorts = new HashMap<>();
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, HierarchicPreferences> children = new HashMap<>();


    private static void assertValid(String text) {
        if (text.matches(".*[" + RESERVED_CHARS + "].*"))
            throw new RuntimeException(text + " contains reserved character " + RESERVED_CHARS);
    }

    public static Optional<HierarchicPreferences> from(Preferences preferences, String key) {
        assertValid(key);
        var res = new HierarchicPreferences();
        res.loadInternal(preferences, key);
        return res.isEmpty() ? Optional.empty() : Optional.of(res);
    }

    private <T> T getGeneric(Map<String, T> map, String key) {
        return map.get(key);
    }

    private <T> void setGeneric(Map<String, T> map, String key, T value) {
        assertValid(key);
        if (value instanceof String) {
            assertValid((String) value);
        }
        map.put(key, value);
    }

    private <T> void deleteGeneric(Map<String, T> map, String key) {
        map.remove(key);
    }

    private <T> Set<String> genericKeys(Map<String, T> map) {
        return map.keySet().stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getString(String key) {
        return getGeneric(strings, key);
    }

    @Override
    public void setString(String key, String value) {
        setGeneric(strings, key, value);
    }

    @Override
    public void deleteString(String key) {
        deleteGeneric(booleans, key);
    }

    @Override
    public Set<String> stringKeys() {
        return genericKeys(strings);
    }

    @Override
    public Boolean getBoolean(String key) {
        return getGeneric(booleans, key);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        setGeneric(booleans, key, value);
    }

    @Override
    public void deleteBoolean(String key) {
        deleteGeneric(booleans, key);
    }

    @Override
    public Set<String> booleanKeys() {
        return genericKeys(booleans);
    }

    @Override
    public Byte getByte(String key) {
        return getGeneric(bytes, key);
    }

    @Override
    public void setByte(String key, byte value) {
        setGeneric(bytes, key, value);
    }

    @Override
    public void deleteByte(String key) {
        deleteGeneric(bytes, key);
    }

    @Override
    public Set<String> byteKeys() {
        return genericKeys(bytes);
    }

    @Override
    public Short getShort(String key) {
        return getGeneric(shorts, key);
    }

    @Override
    public void setShort(String key, short value) {
        setGeneric(shorts, key, value);
    }

    @Override
    public void deleteShort(String key) {
        deleteGeneric(shorts, key);
    }

    @Override
    public Set<String> shortKeys() {
        return genericKeys(shorts);
    }

    @Override
    public Integer getInteger(String key) {
        return getGeneric(integers, key);
    }

    @Override
    public void setInteger(String key, int value) {
        setGeneric(integers, key, value);
    }

    @Override
    public void deleteInteger(String key) {
        deleteGeneric(integers, key);
    }

    @Override
    public Set<String> integerKeys() {
        return genericKeys(integers);
    }

    @Override
    public Long getLong(String key) {
        return getGeneric(longs, key);
    }

    @Override
    public void setLong(String key, long value) {
        setGeneric(longs, key, value);
    }

    @Override
    public void deleteLong(String key) {
        deleteGeneric(longs, key);
    }

    @Override
    public Set<String> longKeys() {
        return genericKeys(longs);
    }

    public HierarchicPreferences getChildObject(String key) {
        return getGeneric(children, key);
    }

    public void setChildObject(String key, HierarchicPreferences value) {
        setGeneric(children, key, value);
    }

    public void deleteChildObject(String key) {
        deleteGeneric(children, key);
    }

    public Set<String> childObjectKeys() {
        return genericKeys(children);
    }

    private <T> void storeGeneric(Preferences preferences, String prefix, String typeName, BiConsumer<String, T> storage, Map<String, T> map) {
        if (map.isEmpty())
            return;

        preferences.setString(prefix + KEY_SEPARATOR + typeName, String.join(TYPE_VALUE_SEPARATOR, map.keySet()));
        for (var entry : map.entrySet()) {
            storage.accept(prefix + KEY_SEPARATOR + typeName + TYPE_VALUE_SEPARATOR + entry.getKey(), entry.getValue());
        }
    }

    private <T> void loadGeneric(Preferences preferences, String prefix, String typeName, Function<String, T> storage, Map<String, T> map) {
        var keys = preferences.getString(prefix + KEY_SEPARATOR + typeName);
        if (keys == null)
            return;
        for (var key : keys.split("\\Q" + TYPE_VALUE_SEPARATOR + "\\E")) {
            map.put(key, storage.apply(prefix + KEY_SEPARATOR + typeName + TYPE_VALUE_SEPARATOR + key));
        }
    }

    private void storeInternal(Preferences preferences, String prefix) {
        storeGeneric(preferences, prefix, BOOLEAN_TYPE, preferences::setBoolean, booleans);
        storeGeneric(preferences, prefix, BYTE_TYPE, preferences::setByte, bytes);
        storeGeneric(preferences, prefix, INTEGER_TYPE, preferences::setInteger, integers);
        storeGeneric(preferences, prefix, LONG_TYPE, preferences::setLong, longs);
        storeGeneric(preferences, prefix, SHORT_TYPE, preferences::setShort, shorts);
        storeGeneric(preferences, prefix, STRING_TYPE, preferences::setString, strings);

        if (children.isEmpty())
            return;
        preferences.setString(prefix + KEY_SEPARATOR + CHILD_TYPE, String.join(TYPE_VALUE_SEPARATOR, children.keySet()));
        for (var entry : children.entrySet()) {
            // avoid key validation
            entry.getValue().storeInternal(preferences, prefix + KEY_SEPARATOR + CHILD_TYPE + TYPE_VALUE_SEPARATOR + entry.getKey());
        }
    }

    public Map<PreferenceType, Set<String>> store(Preferences preferences, String key) {
        assertValid(key);
        if (isEmpty())
            throw new RuntimeException("trying to store empty object");

        WrappedKeyTrackingPreferences wrappedPref = new WrappedKeyTrackingPreferences(preferences);
        storeInternal(wrappedPref, key);
        return wrappedPref.getKeysByType();
    }

    private void loadInternal(Preferences preferences, String prefix) {
        loadGeneric(preferences, prefix, BOOLEAN_TYPE, preferences::getBoolean, booleans);
        loadGeneric(preferences, prefix, BYTE_TYPE, preferences::getByte, bytes);
        loadGeneric(preferences, prefix, INTEGER_TYPE, preferences::getInteger, integers);
        loadGeneric(preferences, prefix, LONG_TYPE, preferences::getLong, longs);
        loadGeneric(preferences, prefix, SHORT_TYPE, preferences::getShort, shorts);
        loadGeneric(preferences, prefix, STRING_TYPE, preferences::getString, strings);

        var keys = preferences.getString(prefix + KEY_SEPARATOR + CHILD_TYPE);
        if (keys == null)
            return;
        for (var key : keys.split("\\Q" + TYPE_VALUE_SEPARATOR + "\\E")) {
            var child = new HierarchicPreferences();
            children.put(key, child);
            child.loadInternal(preferences, prefix + KEY_SEPARATOR + CHILD_TYPE + TYPE_VALUE_SEPARATOR + key);
        }
    }

    private boolean isEmpty() {
        return booleans.isEmpty() && bytes.isEmpty() && integers.isEmpty() && longs.isEmpty() && shorts.isEmpty() && strings.isEmpty() && children.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HierarchicPreferences o))
            return false;
        if (!(booleans.equals(o.booleans) && bytes.equals(o.bytes) && integers.equals(o.integers) && longs.equals(o.longs) && shorts.equals(o.shorts) && strings.equals(o.strings) && children.keySet().equals(o.children.keySet())))
            return false;
        for (var entry : children.entrySet()) {
            if (!entry.getValue().equals(o.children.get(entry.getKey())))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleans, bytes, integers, longs, shorts, strings, children);
    }

    private static class WrappedKeyTrackingPreferences implements Preferences {
        private final Preferences preferences;
        private final Map<PreferenceType, Set<String>> keysByType = new HashMap<>();

        public WrappedKeyTrackingPreferences(Preferences preferences) {
            this.preferences = preferences;
            for (var type : PreferenceType.values()) {
                keysByType.put(type, new HashSet<>());
            }
        }

        public Map<PreferenceType, Set<String>> getKeysByType() {
            return keysByType;
        }

        @Override
        public String getString(String s) {
            return preferences.getString(s);
        }

        @Override
        public void setString(String s, String s1) {
            keysByType.get(PreferenceType.STRING).add(s);
            preferences.setString(s, s1);
        }

        @Override
        public void deleteString(String s) {
            keysByType.get(PreferenceType.STRING).remove(s);
            preferences.deleteString(s);
        }

        @Override
        public Set<String> stringKeys() {
            return preferences.stringKeys();
        }

        @Override
        public Boolean getBoolean(String s) {
            return preferences.getBoolean(s);
        }

        @Override
        public void setBoolean(String s, boolean b) {
            keysByType.get(PreferenceType.BOOLEAN).add(s);
            preferences.setBoolean(s, b);
        }

        @Override
        public void deleteBoolean(String s) {
            keysByType.get(PreferenceType.BOOLEAN).remove(s);
            preferences.deleteBoolean(s);
        }

        @Override
        public Set<String> booleanKeys() {
            return preferences.booleanKeys();
        }

        @Override
        public Byte getByte(String s) {
            return preferences.getByte(s);
        }

        @Override
        public void setByte(String s, byte b) {
            keysByType.get(PreferenceType.BYTE).add(s);
            preferences.setByte(s, b);
        }

        @Override
        public void deleteByte(String s) {
            keysByType.get(PreferenceType.BYTE).remove(s);
            preferences.deleteByte(s);
        }

        @Override
        public Set<String> byteKeys() {
            return preferences.byteKeys();
        }

        @Override
        public Short getShort(String s) {
            return preferences.getShort(s);
        }

        @Override
        public void setShort(String s, short i) {
            keysByType.get(PreferenceType.SHORT).add(s);
            preferences.setShort(s, i);
        }

        @Override
        public void deleteShort(String s) {
            keysByType.get(PreferenceType.SHORT).remove(s);
            preferences.deleteShort(s);
        }

        @Override
        public Set<String> shortKeys() {
            return preferences.shortKeys();
        }

        @Override
        public Integer getInteger(String s) {
            return preferences.getInteger(s);
        }

        @Override
        public void setInteger(String s, int i) {
            keysByType.get(PreferenceType.INTEGER).add(s);
            preferences.setInteger(s, i);
        }

        @Override
        public void deleteInteger(String s) {
            keysByType.get(PreferenceType.INTEGER).remove(s);
            preferences.deleteInteger(s);
        }

        @Override
        public Set<String> integerKeys() {
            return preferences.integerKeys();
        }

        @Override
        public Long getLong(String s) {
            return preferences.getLong(s);
        }

        @Override
        public void setLong(String s, long l) {
            keysByType.get(PreferenceType.LONG).add(s);
            preferences.setLong(s, l);
        }

        @Override
        public void deleteLong(String s) {
            keysByType.get(PreferenceType.LONG).remove(s);
            preferences.deleteLong(s);
        }

        @Override
        public Set<String> longKeys() {
            return preferences.longKeys();
        }
    }
}

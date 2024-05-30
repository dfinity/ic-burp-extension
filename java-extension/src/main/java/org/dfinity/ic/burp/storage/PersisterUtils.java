package org.dfinity.ic.burp.storage;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Preferences;
import org.dfinity.ic.burp.model.PreferenceType;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PersisterUtils {

    private final Map<PreferenceType, PrefMethods> prefMethodsByType;

    public PersisterUtils(Preferences preferences) {
        prefMethodsByType = Map.of(
                PreferenceType.BOOLEAN, new PrefMethods(preferences::booleanKeys, preferences::deleteBoolean),
                PreferenceType.BYTE, new PrefMethods(preferences::byteKeys, preferences::deleteByte),
                PreferenceType.INTEGER, new PrefMethods(preferences::integerKeys, preferences::deleteInteger),
                PreferenceType.LONG, new PrefMethods(preferences::longKeys, preferences::deleteLong),
                PreferenceType.SHORT, new PrefMethods(preferences::shortKeys, preferences::deleteShort),
                PreferenceType.STRING, new PrefMethods(preferences::stringKeys, preferences::deleteString)
        );
    }

    /**
     * Logs all preferences to output sorted by type. This is helpful for debugging.
     *
     * @param log burp logger used for logging the preferences
     */
    public void logPreferences(Logging log) {
        var ctr = 0;
        log.logToOutput("--- DUMP preference keys start ---");
        for (var entry : prefMethodsByType.entrySet()) {
            var keys = entry.getValue().keyProvider.get();
            if (!keys.isEmpty()) {
                log.logToOutput("type = " + entry.getKey());
            }
            for (var key : keys) {
                ctr++;
                log.logToOutput(key);
            }
        }
        log.logToOutput("num keys = " + ctr + "\n--- DUMP preference keys end ---");
    }

    /**
     * Deletes all preferences where the provided matcher returns true.
     *
     * @param matcher gets the preference type and a key and should return true iff the corresponding entry should be deleted
     */
    public void deleteMatchingPreferences(BiPredicate<PreferenceType, String> matcher) {
        for (var entry : prefMethodsByType.entrySet()) {
            for (var key : entry.getValue().keyProvider.get()) {
                if (matcher.test(entry.getKey(), key)) {
                    entry.getValue().keyEraser.accept(key);
                }
            }
        }
    }


    record PrefMethods(
            Supplier<Set<String>> keyProvider,
            Consumer<String> keyEraser
    ) {
    }
}

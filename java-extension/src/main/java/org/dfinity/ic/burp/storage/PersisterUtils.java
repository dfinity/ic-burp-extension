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

    private final Map<PreferenceType, Bundle> types;

    public PersisterUtils(Preferences preferences) {
        types = Map.of(
                PreferenceType.BOOLEAN, new Bundle(preferences::booleanKeys, preferences::deleteBoolean),
                PreferenceType.BYTE, new Bundle(preferences::byteKeys, preferences::deleteByte),
                PreferenceType.INTEGER, new Bundle(preferences::integerKeys, preferences::deleteInteger),
                PreferenceType.LONG, new Bundle(preferences::longKeys, preferences::deleteLong),
                PreferenceType.SHORT, new Bundle(preferences::shortKeys, preferences::deleteShort),
                PreferenceType.STRING, new Bundle(preferences::stringKeys, preferences::deleteString)
        );
    }

    public void logPreferences(Logging log) {
        var ctr = 0;
        log.logToOutput("--- DUMP preference keys start ---");
        for (var entry : types.entrySet()) {
            var keys = entry.getValue().keyProvider.get();
            if (!keys.isEmpty()) {
                log.logToOutput("type = " + entry.getKey());
            }
            for (var key : keys) {
                ctr++;
                log.logToOutput(key);
                //pr.delete.accept(key);
            }
        }
        log.logToOutput("num keys = " + ctr + "\n--- DUMP preference keys end ---");
    }

    /**
     * Deletes all preferences where the provided matcher returns true.
     *
     * @param matcher matcher that gets the preference type and a key and should return true iff the corresponding entry should be deleted.
     */
    public void deleteMatchingPreferences(BiPredicate<PreferenceType, String> matcher) {
        for (var entry : types.entrySet()) {
            for (var key : entry.getValue().keyProvider.get()) {
                if (matcher.test(entry.getKey(), key)) {
                    entry.getValue().deleteExecutor.accept(key);
                }
            }
        }
    }


    record Bundle(
            Supplier<Set<String>> keyProvider,
            Consumer<String> deleteExecutor
    ) {
    }
}

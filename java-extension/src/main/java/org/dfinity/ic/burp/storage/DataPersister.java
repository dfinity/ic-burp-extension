package org.dfinity.ic.burp.storage;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jose.JOSEException;
import org.dfinity.ic.burp.UI.CacheLoaderSubscriber;
import org.dfinity.ic.burp.model.CanisterCacheInfo;
import org.dfinity.ic.burp.model.IiState;
import org.dfinity.ic.burp.model.InternetIdentities;
import org.dfinity.ic.burp.model.InternetIdentity;
import org.dfinity.ic.burp.model.JWKIdentity;
import org.dfinity.ic.burp.model.PreferenceType;
import org.dfinity.ic.burp.tools.IcTools;
import org.dfinity.ic.burp.tools.model.IcToolsException;
import org.dfinity.ic.burp.tools.model.InterfaceType;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.dfinity.ic.burp.storage.HierarchicPreferences.KEY_SEPARATOR;

public class DataPersister {
    public static final String IDENTITIES_KEY = "Identities";
    public static final String CANISTER_INTERFACE_CACHE_KEY = "CanisterInterfaceCache";
    public static final String IC_KEY = "IC";
    public static final String IDL_KEY = "IDL";
    public static final String ACTIVE_CANISTER_INTERFACE_TYPE_KEY = "ActiveCanisterInterfaceType";
    public static final String ACTIVATION_DATE_KEY = "ActivationDate";
    public static final String CREATION_DATE_KEY = "CreationDate";
    public static final String PASSKEY_KEY = "Passkey";
    public static final String STATE_KEY = "State";
    private final PersistedObject rootPO;
    private final Preferences preferences;
    private final IcTools icTools;
    private final Logging log;
    private final CacheLoaderSubscriber cacheLoaderSubscriber;
    private JWKIdentity defaultIdentity;


    public DataPersister(Logging log, IcTools icTools, PersistedObject rootPO, Preferences preferences, CacheLoaderSubscriber cacheLoaderSubscriber) {
        this.rootPO = rootPO;
        this.preferences = preferences;
        this.log = log;
        this.icTools = icTools;
        this.cacheLoaderSubscriber = cacheLoaderSubscriber;
    }

    public AsyncLoadingCache<String, CanisterCacheInfo> getCanisterInterfaceCache() {

        AsyncLoadingCache<String, CanisterCacheInfo> cache = Caffeine.newBuilder().buildAsync(
                cid -> {
                    if (cid == null) {
                        log.logToError("Argument provided to the cache is not a String.");
                        return null;
                    }
                    log.logToOutput("Resolving canister interface for: " + cid);
                    CanisterCacheInfo val;
                    try {
                        Optional<String> idlOpt = icTools.discoverCanisterInterface(cid);
                        InterfaceType t = idlOpt.isPresent() ? InterfaceType.AUTOMATIC : InterfaceType.FAILED;
                        String idl = idlOpt.orElse("");
                        val = new CanisterCacheInfo(idl, t);
                    } catch (IcToolsException e) {
                        log.logToError(String.format("discoverCanisterInterface failed for canisterId %s", cid), e);
                        val = new CanisterCacheInfo("", InterfaceType.FAILED);
                    }
                    cacheLoaderSubscriber.onCacheLoad();
                    return val;
                }
        );
        log.logToOutput("rootPO child object keys: " + rootPO.childObjectKeys());
        PersistedObject icPO = rootPO.getChildObject(IC_KEY);
        if (icPO == null) return cache;
        log.logToOutput("icPO child object keys: " + icPO.childObjectKeys());
        PersistedObject canisterInterfaceCachePO = icPO.getChildObject(CANISTER_INTERFACE_CACHE_KEY);
        if (canisterInterfaceCachePO == null) return cache;

        log.logToOutput("Adding the following CIDs to cache from persistent storage: \n" + canisterInterfaceCachePO.childObjectKeys());

        for (String cid : canisterInterfaceCachePO.childObjectKeys()) {
            PersistedObject canisterCacheInfoPO = canisterInterfaceCachePO.getChildObject(cid);
            if (canisterCacheInfoPO == null) continue; // This shouldn't happen as we just fetched the available keys.
            CanisterCacheInfo info = new CanisterCacheInfo();

            log.logToOutput("ActiveCanisterInterfaceType from storage: " + canisterCacheInfoPO.getString(ACTIVE_CANISTER_INTERFACE_TYPE_KEY));
            try {
                InterfaceType type = InterfaceType.valueOf(canisterCacheInfoPO.getString(ACTIVE_CANISTER_INTERFACE_TYPE_KEY));
                info.setActiveCanisterInterfaceType(type);
            } catch (Exception e) {
                log.logToError("Active canister interface type could not be deserialized to enum.", e);
            }
            log.logToOutput("nextPO keys: " + canisterCacheInfoPO.childObjectKeys());
            for (String type : canisterCacheInfoPO.childObjectKeys()) {
                PersistedObject canisterInterfacePO = canisterCacheInfoPO.getChildObject(type);
                if (canisterInterfacePO == null)
                    continue; // This shouldn't happen as we just fetched the available keys.
                String idl = canisterInterfacePO.getString(IDL_KEY);
                idl = idl == null ? "" : idl;
                try {
                    info.putCanisterInterface(idl, InterfaceType.valueOf(type));
                } catch (Exception e) {
                    log.logToError("Canister interface type could not be deserialized to enum. The IDL was not restored properly for canister: " + cid, e);
                }
            }
            cache.synchronous().put(cid, info);
        }
        return cache;
    }

    public boolean storeCanisterInterfaceCache(AsyncLoadingCache<String, CanisterCacheInfo> cache) {
        /* The strategy is to create a new PersistedObject.
            Overall, the data structure looks like a tree:

         BurpRoot (PersistedObject)
                    |
                    |
         ICObject (PersistedObject) ---------------------------------
                    |                                               |
                    |                                               |
        CanisterInterfaceCache (PersistedObject)                Identities (PersistedObject)
                    |                                               |
                    |   Key = CID                                  ... (See storeInternetIdentities)
                    |
            CanisterCacheInfo (PersistedObject): Each such persisted object will have the activeCanisterInterfaceType serialized to a String.
                    |                            The key for activeCanisterInterfaceType is "ActiveCanisterInterfaceType".
                    |   Key = InterfaceType.toString()
                    |
            CanisterInterface (PersistedObject)
                    |
                    |   Key = "IDL"
                    |
                   IDL (String): This is the IDL content.
        */

        log.logToOutput("Storing IDLs to Burp project file.");
        generatePOTree();
        PersistedObject icPO = this.rootPO.getChildObject(IC_KEY);
        PersistedObject canisterInterfaceCachePO = icPO.getChildObject(CANISTER_INTERFACE_CACHE_KEY);

        for (Map.Entry<String, CanisterCacheInfo> cacheEntry : cache.synchronous().asMap().entrySet()) {
            PersistedObject canisterCacheInfoPO = PersistedObject.persistedObject();
            CanisterCacheInfo info = cacheEntry.getValue();
            canisterCacheInfoPO.setString(ACTIVE_CANISTER_INTERFACE_TYPE_KEY, info.getActiveCanisterInterfaceType().name());

            for (Map.Entry<InterfaceType, String> canisterInterface : info.getCanisterInterfaces().entrySet()) {
                PersistedObject canisterInterfacePO = PersistedObject.persistedObject();
                canisterInterfacePO.setString(IDL_KEY, canisterInterface.getValue());
                canisterCacheInfoPO.setChildObject(canisterInterface.getKey().name(), canisterInterfacePO);
            }
            canisterInterfaceCachePO.setChildObject(cacheEntry.getKey(), canisterCacheInfoPO);
        }
        return true;
    }

    public boolean clearCanisterInterfaceCache() {
        PersistedObject icPO = PersistedObject.persistedObject();
        this.rootPO.setChildObject(IC_KEY, icPO);
        return true;
    }

    private void generatePOTree() {
        PersistedObject icPO = this.rootPO.getChildObject(IC_KEY);
        if (icPO == null) icPO = PersistedObject.persistedObject();

        PersistedObject canisterInterfaceCachePO = icPO.getChildObject(CANISTER_INTERFACE_CACHE_KEY);
        if (canisterInterfaceCachePO == null) canisterInterfaceCachePO = PersistedObject.persistedObject();
        icPO.setChildObject(CANISTER_INTERFACE_CACHE_KEY, canisterInterfaceCachePO);

        this.rootPO.setChildObject(IC_KEY, icPO);
    }

    public JWKIdentity getDefaultIdentity() {
        // TODO The default JWK Identity is not yet persisted. A new one is generated when loading the extension.
        // PersistedObject icObject = rootPO.getChildObject("IC");

        try {
            this.defaultIdentity = new JWKIdentity(log);
        } catch (JOSEException e) {
            log.logToError("Error generating default identity.", e);
            return null;
        }

        return this.defaultIdentity;
    }

    public void storeInternetIdentities(InternetIdentities identities) {
        /* The strategy is to create a new HierarchicPreferences.
           Overall, the data structure looks like a tree:

                                             Burp Preferences (Preferences)
                                                        |
                                                        |
                                             ICObject (HierarchicPreferences)
                                                        |
                                                        |
                                                Identities (HierarchicPreferences)
                                                        |
                                                        |   Key = Anchor
                                                        |
                                              II (HierarchicPreferences)
                                                        |
                     -----------------------------------|------------------------------------------
                     |                    |                           |                           |
         Key="State" |      Key="Passkey" |        Key="CreationDate" |      Key="ActivationDate" |
         Type=String |      Type=String   |        Type=Long          |      Type=Long            |
                     |                    |                           |                           |
       Name of the IiState enum   Pem file of the passkey    Date II was created      Date the passkey was activated
                                                                                      and not present if not yet activated.
        */
        Map<PreferenceType, Set<String>> createdKeys;
        if (identities.getIdentities().isEmpty()) {
            createdKeys = Map.of();
        } else {
            log.logToOutput("Storing identities to Burp preference file.");
            HierarchicPreferences icPref = new HierarchicPreferences();
            HierarchicPreferences identitiesPref = new HierarchicPreferences();
            icPref.setChildObject(IDENTITIES_KEY, identitiesPref);

            for (Map.Entry<String, InternetIdentity> iiEntry : identities.getIdentities().entrySet()) {
                log.logToOutput("Storing identities with anchor: " + iiEntry.getKey());
                HierarchicPreferences iiPref = new HierarchicPreferences();
                InternetIdentity ii = iiEntry.getValue();

                iiPref.setString(STATE_KEY, ii.getState().name());
                // The pem file of a passkey should never be empty.
                iiPref.setString(PASSKEY_KEY, ii.getPasskey().getPem().orElseThrow());
                iiPref.setLong(CREATION_DATE_KEY, ii.creationDate().getTime());
                Optional<Date> activationDate = ii.activationDate();
                activationDate.ifPresent(date -> iiPref.setLong(ACTIVATION_DATE_KEY, date.getTime()));
                log.logToOutput("Adding identity with: ("
                        + iiEntry.getKey() + ", "
                        + ii.getPasskey().getPem().orElseThrow() + ", "
                        + ii.getState().name() + ", "
                        + ii.creationDate() + ", "
                        + activationDate + ")");

                log.logToOutput("Storing identities with HierarchicPreferences: " + iiPref);
                identitiesPref.setChildObject(iiEntry.getKey(), iiPref);
            }
            createdKeys = icPref.store(preferences, IC_KEY);
        }

        new PersisterUtils(preferences).deleteMatchingPreferences((type, key) -> {
            if (!key.startsWith(IC_KEY + KEY_SEPARATOR)) {
                // we do not delete preferences outside our namespace
                return false;
            }
            return !createdKeys.getOrDefault(type, Set.of()).contains(key);
        });
    }

    public InternetIdentities getInternetIdentities() {
        InternetIdentities identities = new InternetIdentities(this.log, this.icTools);

        Optional<HierarchicPreferences> icPrefOpt = HierarchicPreferences.from(preferences, IC_KEY);
        if (icPrefOpt.isEmpty()) return identities;
        HierarchicPreferences icPref = icPrefOpt.get();

        log.logToOutput("icPref child object keys: " + icPref.childObjectKeys());
        HierarchicPreferences identitiesPref = icPref.getChildObject(IDENTITIES_KEY);
        if (identitiesPref == null) return identities;

        log.logToOutput("Adding the following anchors to identities from persistent storage: \n" + identitiesPref.childObjectKeys());

        for (String anchor : identitiesPref.childObjectKeys()) {
            HierarchicPreferences iiPref = identitiesPref.getChildObject(anchor);
            if (iiPref == null) continue; // This shouldn't happen as we just fetched the available keys.

            IiState state;
            try {
                state = IiState.valueOf(iiPref.getString(STATE_KEY));
            } catch (IllegalArgumentException e) {
                // This can happen if the persistent storage was changed manually or if a iiState name was changed or removed.
                this.log.logToError("Could not find IiState with anchor " + anchor + " and state: " + iiPref.getString(STATE_KEY));
                continue;
            }

            String passKeyPem = iiPref.getString(PASSKEY_KEY);
            Date creationDate = new Date(iiPref.getLong(CREATION_DATE_KEY));
            Long activationDateLong = iiPref.getLong(ACTIVATION_DATE_KEY);
            Date activationDate = activationDateLong == null ? null : new Date(activationDateLong);

            try {
                this.log.logToOutput("Adding identity with: (" + anchor + ", " + passKeyPem + ", " + state + ", " + creationDate + ", " + activationDate + ")");
                identities.addIdentity(anchor, passKeyPem, state, creationDate, activationDate);
            } catch (IcToolsException e) {
                this.log.logToError("Could not create InternetIdentity with anchor " + anchor + "\nException: " + e);
            }
        }
        return identities;
    }
}

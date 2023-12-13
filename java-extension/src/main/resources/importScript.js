// Paste this script in the console of the browser tab where the target dApp is loaded.
const jwkEcKey = <PLACEHOLDER>;

window.crypto.subtle.importKey(
    "jwk",
    jwkEcKey,
        {
            name: "ECDSA",
            namedCurve: "P-256"
        },
    true,
    ["sign"]
).then((pair) => {
        var connection = indexedDB.open('auth-client-db',1);
        connection.onsuccess = (e) => {
        var db = e.target.result;
        var trx = db.transaction('ic-keyval', 'readwrite');
        var store = trx.objectStore('ic-keyval');
        store.delete('delegation');
        store.put(pair,'identity');
    };
});
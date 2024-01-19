use std::sync::Arc;

use candid::{Decode, Encode, Principal};
use ic_agent::{Agent, Identity};
use serde_bytes::ByteBuf;

use crate::internet_identity::model::{AddTentativeDeviceResponse, AnchorCredentials, DelegationInfo, DeviceData, DeviceProtection, GetDelegationResponse, InternetIdentityResult, KeyType, Purpose, Timestamp, UserKey};
use crate::internet_identity::model::InternetIdentityError::{AddTentativePasskeyFailed, GetDelegationFailed, PublicKeyExtractionFailed};

pub mod model;

const II_CANISTER_ID: &str = "rdmx6-jaaaa-aaaaa-aaadq-cai";

pub async fn internet_identity_add_tentative_passkey(anchor: String, sign_identity: Arc<dyn Identity>) -> InternetIdentityResult<String> {
    let agent = Agent::builder()
        .with_url("https://ic0.app")
        .build()?;

    let device = DeviceData {
        pubkey: ByteBuf::from(sign_identity.public_key().ok_or_else(|| PublicKeyExtractionFailed("the provided identity doesn't have a public key".to_string()))?),
        alias: "Burp".to_string(),
        credential_id: Some(ByteBuf::from(vec![])),
        purpose: Purpose::Authentication,
        key_type: KeyType::Platform,
        protection: DeviceProtection::Unprotected,
        origin: None,
        metadata: None,
    };
    let res = agent
        .update(&Principal::from_text(II_CANISTER_ID)?, "add_tentative_device")
        .with_arg(Encode!(&anchor.parse::<u64>()?, &device)?)
        .call_and_wait()
        .await?;
    let add_device_reply = Decode!(res.as_slice(), AddTentativeDeviceResponse)?;
    match add_device_reply {
        AddTentativeDeviceResponse::AddedTentatively { verification_code, .. } => {
            Ok(verification_code)
        }
        AddTentativeDeviceResponse::DeviceRegistrationModeOff => {
            Err(AddTentativePasskeyFailed("Device registration is turned off".to_string()))
        }
        AddTentativeDeviceResponse::AnotherDeviceTentativelyAdded => {
            Err(AddTentativePasskeyFailed("Another tentative device was already added".to_string()))
        }
    }
}

pub async fn internet_identity_is_passkey_registered(anchor: String, sign_identity: Arc<dyn Identity>) -> InternetIdentityResult<bool> {
    let agent = Agent::builder()
        .with_url("https://ic0.app")
        .build()?;
    let target_key = ByteBuf::from(sign_identity.public_key().ok_or_else(|| PublicKeyExtractionFailed("the provided identity doesn't have a public key".to_string()))?);

    let res = agent
        .query(&Principal::from_text(II_CANISTER_ID)?, "get_anchor_credentials")
        .with_arg(Encode!(&anchor.parse::<u64>()?)?)
        .call()
        .await?;
    let anchor_credentials = Decode!(res.as_slice(), AnchorCredentials)?;
    for credential in anchor_credentials.credentials {
        if target_key.eq(&credential.pubkey) {
            return Ok(true);
        }
    }
    Ok(false)
}

pub async fn internet_identity_get_principal(anchor: String, sign_identity: Arc<dyn Identity>, frontend_hostname: String) -> InternetIdentityResult<Principal> {
    let agent = Agent::builder()
        .with_url("https://ic0.app")
        .with_identity(sign_identity)
        .build()?;

    let res = agent
        .query(&Principal::from_text(II_CANISTER_ID)?, "get_principal")
        .with_arg(Encode!(&anchor.parse::<u64>()?, &frontend_hostname)?)
        .call()
        .await?;

    Ok(Decode!(res.as_slice(), Principal)?)
}

pub async fn internet_identity_get_delegation(anchor: String, sign_identity: Arc<dyn Identity>, frontend_hostname: String, session_identity: Arc<dyn Identity>) -> InternetIdentityResult<DelegationInfo> {
    let agent = Agent::builder()
        .with_url("https://ic0.app")
        .with_identity(sign_identity)
        .build()?;
    let anchor_u64 = anchor.parse::<u64>()?;
    let session_key = ByteBuf::from(session_identity.public_key().ok_or_else(|| PublicKeyExtractionFailed("the provided identity doesn't have a public key".to_string()))?);

    let res = agent
        .update(&Principal::from_text(II_CANISTER_ID)?, "prepare_delegation")
        .with_arg(Encode!(&anchor_u64, &frontend_hostname, &session_key, &Some(u64::MAX))?)
        .call_and_wait()
        .await?;
    let (user, expiry) = Decode!(res.as_slice(), UserKey, Timestamp)?;

    let res = agent
        .query(&Principal::from_text(II_CANISTER_ID)?, "get_delegation")
        .with_arg(Encode!(&anchor_u64, &frontend_hostname, &session_key, &expiry)?)
        .call()
        .await?;
    let delegation_response = Decode!(res.as_slice(), GetDelegationResponse)?;
    match delegation_response {
        GetDelegationResponse::SignedDelegation(delegation) =>
            Ok(DelegationInfo {
                from_pubkey: user.into_vec(),
                delegation_chain: vec![delegation.into()],
            }),
        GetDelegationResponse::NoSuchDelegation => Err(GetDelegationFailed()),
    }
}


#[cfg(test)]
mod tests {
    use std::io;
    use std::io::Write;
    use std::sync::Arc;
    use std::time::Duration;

    use base64::Engine;
    use candid::{Decode, Encode, Principal};
    use ic_agent::{Agent, Identity};
    use ic_agent::identity::{BasicIdentity, DelegatedIdentity};
    use ring::signature::Ed25519KeyPair;

    use crate::internet_identity::{internet_identity_add_tentative_passkey, internet_identity_get_delegation, internet_identity_get_principal, internet_identity_is_passkey_registered};

    fn gen_identity() -> String {
        match Ed25519KeyPair::generate_pkcs8(&ring::rand::SystemRandom::new()) {
            Ok(pem_doc) => {
                let pem_enc = base64::engine::general_purpose::STANDARD.encode(pem_doc.as_ref());
                let pem_export = format!("-----BEGIN PRIVATE KEY-----\n{}\n-----END PRIVATE KEY-----", pem_enc);
                pem_export
            }
            Err(e) => {
                panic!("{}", e.to_string())
            }
        }
    }

    // this test requires user input, it needs to be run from the terminal with the following command:
    // > cargo test --package rust-lib --lib internet_identity::tests::register_passkey_and_login -- --nocapture
    #[tokio::test]
    #[ignore]
    async fn register_passkey_and_login() {
        let sign_identity_pem = gen_identity();
        let sign_identity: Arc<dyn Identity> = Arc::new(BasicIdentity::from_pem(sign_identity_pem.as_bytes()).unwrap());
        println!("Internet Identity Manual Integration Test");
        println!("1. Login to internet identity and click on \"Add new Passkey\"");
        print!("2. Enter anchor: ");
        io::stdout().flush().unwrap();
        let mut buf = String::new();
        io::stdin().read_line(&mut buf).unwrap();
        let anchor = String::from(buf.trim());
        let code = internet_identity_add_tentative_passkey(anchor.clone(), sign_identity.clone()).await.unwrap();
        println!("3. Enter the following code: {}", code);
        print!("Waiting for code submission");
        io::stdout().flush().unwrap();
        while !internet_identity_is_passkey_registered(anchor.clone(), sign_identity.clone()).await.unwrap() {
            print!(".");
            io::stdout().flush().unwrap();
            tokio::time::sleep(Duration::from_secs(1)).await;
        }
        println!("\nThe following passkey was successfully registered:\n{}", sign_identity_pem);

        // get nns principal
        let nns_hostname = String::from("https://nns.ic0.app");
        let nns_principal = internet_identity_get_principal(anchor.clone(), sign_identity.clone(), nns_hostname.clone()).await.unwrap();
        println!("\n\nNNS principal of anchor {}: {}", anchor, nns_principal);

        // get delegation for nns principal
        let session_identity_pem = gen_identity();
        let session_identity: Arc<dyn Identity> = Arc::new(BasicIdentity::from_pem(session_identity_pem.as_bytes()).unwrap());
        let delegation = internet_identity_get_delegation(anchor.clone(), sign_identity.clone(), nns_hostname.clone(), session_identity.clone()).await.unwrap();
        let mut chain: Vec<ic_transport_types::SignedDelegation> = vec![];
        for del in delegation.delegation_chain {
            chain.push(del.into());
        }
        let delegated_identity: Arc<dyn Identity> = Arc::new(DelegatedIdentity::new(delegation.from_pubkey, Box::new(session_identity), chain));

        // call whoami canister with delegated identity
        let agent = Agent::builder()
            .with_url("https://ic0.app")
            .with_identity(delegated_identity)
            .build()
            .unwrap();
        let res = agent.query(&Principal::from_text("ivcos-eqaaa-aaaab-qablq-cai").unwrap(), "whoami").with_arg(Encode!(&()).unwrap()).call().await.unwrap();
        let whoami_principal = Decode!(res.as_slice(), Principal).unwrap();
        println!("\n\nPrincipal returned by whoami canister: {}", whoami_principal);

        assert_eq!(nns_principal, whoami_principal);
    }
}
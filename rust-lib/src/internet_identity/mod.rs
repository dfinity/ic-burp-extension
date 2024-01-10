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

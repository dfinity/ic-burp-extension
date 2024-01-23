use std::collections::HashMap;
use std::num::ParseIntError;

use candid::{CandidType, Deserialize, Principal};
use candid::types::principal::PrincipalError;
use ic_agent::AgentError;
use serde_bytes::ByteBuf;
use thiserror::Error;

use crate::encode::model::RequestSenderDelegation;

pub type CredentialId = ByteBuf;
pub type PublicKey = ByteBuf;
pub type DeviceKey = PublicKey;
pub type UserKey = PublicKey;
// in nanos since epoch
pub type Timestamp = u64;
pub type Signature = ByteBuf;

#[derive(Eq, PartialEq, Clone, Debug, CandidType, Deserialize)]
pub enum Purpose {
    #[serde(rename = "recovery")]
    Recovery,
    #[serde(rename = "authentication")]
    Authentication,
}

#[derive(Eq, PartialEq, Clone, Debug, CandidType, Deserialize)]
pub enum KeyType {
    #[serde(rename = "unknown")]
    Unknown,
    #[serde(rename = "platform")]
    Platform,
    #[serde(rename = "cross_platform")]
    CrossPlatform,
    #[serde(rename = "seed_phrase")]
    SeedPhrase,
    #[serde(rename = "browser_storage_key")]
    BrowserStorageKey,
}

#[derive(Eq, PartialEq, Clone, Debug, CandidType, Deserialize)]
pub enum DeviceProtection {
    #[serde(rename = "protected")]
    Protected,
    #[serde(rename = "unprotected")]
    Unprotected,
}

#[derive(Eq, PartialEq, Clone, Debug, CandidType, Deserialize)]
pub enum MetadataEntry {
    #[serde(rename = "string")]
    String(String),
    #[serde(rename = "bytes")]
    Bytes(ByteBuf),
    #[serde(rename = "map")]
    Map(HashMap<String, MetadataEntry>),
}

#[derive(Eq, PartialEq, Clone, Debug, CandidType, Deserialize)]
pub struct DeviceData {
    pub pubkey: DeviceKey,
    pub alias: String,
    pub credential_id: Option<CredentialId>,
    pub purpose: Purpose,
    pub key_type: KeyType,
    pub protection: DeviceProtection,
    pub origin: Option<String>,
    // Metadata map for additional device information.
    //
    // Note: some fields above will be moved to the metadata map in the future.
    // All field names of `DeviceData` (such as 'alias', 'origin, etc.) are
    // reserved and cannot be written.
    pub metadata: Option<HashMap<String, MetadataEntry>>,
}

pub type DeviceVerificationCode = String;

#[derive(Clone, Debug, CandidType, Deserialize)]
pub enum AddTentativeDeviceResponse {
    #[serde(rename = "added_tentatively")]
    AddedTentatively {
        verification_code: DeviceVerificationCode,
        device_registration_timeout: Timestamp,
    },
    #[serde(rename = "device_registration_mode_off")]
    DeviceRegistrationModeOff,
    #[serde(rename = "another_device_tentatively_added")]
    AnotherDeviceTentativelyAdded,
}

#[derive(Clone, Debug, CandidType, Deserialize, Eq, PartialEq)]
pub struct WebAuthnCredential {
    pub pubkey: DeviceKey,
    pub credential_id: CredentialId,
}

#[derive(Clone, Debug, CandidType, Deserialize, Eq, PartialEq, Default)]
pub struct AnchorCredentials {
    pub credentials: Vec<WebAuthnCredential>,
    pub recovery_credentials: Vec<WebAuthnCredential>,
    pub recovery_phrases: Vec<PublicKey>,
}

#[derive(Clone, Debug, CandidType, Deserialize)]
pub struct Delegation {
    pub pubkey: PublicKey,
    pub expiration: Timestamp,
    pub targets: Option<Vec<Principal>>,
}

impl Into<ic_agent::identity::Delegation> for Delegation {
    fn into(self) -> ic_agent::identity::Delegation {
        ic_agent::identity::Delegation {
            pubkey: self.pubkey.into_vec(),
            expiration: self.expiration,
            targets: self.targets,
        }
    }
}

#[derive(Clone, Debug, CandidType, Deserialize)]
pub struct SignedDelegation {
    pub delegation: Delegation,
    pub signature: Signature,
}


#[derive(Clone, Debug, CandidType, Deserialize)]
pub enum GetDelegationResponse {
    #[serde(rename = "signed_delegation")]
    SignedDelegation(SignedDelegation),
    #[serde(rename = "no_such_delegation")]
    NoSuchDelegation,
}

pub type InternetIdentityResult<T> = Result<T, InternetIdentityError>;

#[derive(Error, Debug)]
pub enum InternetIdentityError {
    #[error(r#"Principal creation from CanisterID failed: {0}"#)]
    PrincipalCreationFailed(#[from] PrincipalError),
    #[error(r#"Could not convert anchor to u64: {0}"#)]
    AnchorConversionFailed(#[from] ParseIntError),
    #[error(r#"Resolving the public key failed: {0}"#)]
    PublicKeyExtractionFailed(String),
    #[error(r#"Calling internet identity failed: {0}"#)]
    InternetIdentityCallFailed(#[from] AgentError),
    #[error(r#"CANDID conversion failed: {0}"#)]
    CandidConversionFailed(#[from] candid::Error),
    #[error("Adding a tentative passkey failed: {0}")]
    AddTentativePasskeyFailed(String),
    #[error("Getting the delegation failed")]
    GetDelegationFailed(),
}

impl From<SignedDelegation> for RequestSenderDelegation {
    fn from(delegation: SignedDelegation) -> Self {
        Self {
            pubkey: delegation.delegation.pubkey.into_vec(),
            expiration: delegation.delegation.expiration,
            targets: match delegation.delegation.targets {
                None => { vec![] }
                Some(t) => { t }
            },
            signature: delegation.signature.into_vec(),
        }
    }
}

pub struct DelegationInfo {
    pub from_pubkey: Vec<u8>,
    pub delegation_chain: Vec<RequestSenderDelegation>,
}
use std::string::FromUtf8Error;

use ic_agent::AgentError;
use ic_agent::export::PrincipalError;
use thiserror::Error;

pub type LookupResult<T> = Result<T, LookupError>;

#[derive(Error, Debug)]
pub enum LookupError {
    #[error(r#"Principal creation from CanisterID failed: {0}"#)]
    PrincipalCreationFailed(#[from] PrincipalError),
    #[error(r#"Retrieving canister metadata failed: {0}"#)]
    RetrievingCanisterMetadataFailed(#[from] AgentError),
    #[error(r#"Canister interface conversion failed: {0}"#)]
    CanisterInterfaceConversionFailed(#[from] FromUtf8Error),
    #[error(r#"CANDID conversion failed: {0}"#)]
    CandidConversionFailed(#[from] candid::Error),
}

pub enum LookupMethod {
    CanisterMetadata,
    CanisterMethod,
}

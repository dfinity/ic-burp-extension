use std::os::raw::c_char;
use std::ptr::null;

use base64::Engine;
use candid::Principal;

use crate::canister_lookup::model::LookupResult;
use crate::encode::EncodingResult;
use crate::encode::model::{RequestInfo, RequestMetadata};
use crate::internet_identity::model::{DelegationInfo, InternetIdentityResult};
use crate::jna::{delegation_to_string, str_to_ptr};

#[repr(C)]
pub struct DiscoverCanisterInterfaceResult {
    error_message: *const c_char,
    canister_interface: *const c_char,
}

impl DiscoverCanisterInterfaceResult {
    pub fn error(err: String) -> Self {
        DiscoverCanisterInterfaceResult {
            error_message: str_to_ptr(err),
            canister_interface: null(),
        }
    }
}

impl From<LookupResult<Option<String>>> for DiscoverCanisterInterfaceResult {
    fn from(result: LookupResult<Option<String>>) -> Self {
        match result {
            Ok(interface_opt) => {
                DiscoverCanisterInterfaceResult {
                    error_message: null(),
                    canister_interface: str_to_ptr(interface_opt.unwrap_or("".to_string())),
                }
            }
            Err(err) => {
                DiscoverCanisterInterfaceResult {
                    error_message: str_to_ptr(err.to_string()),
                    canister_interface: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct GetRequestMetadataResult {
    error_message: *const c_char,
    request_type: *const c_char,
    request_id: *const c_char,
    sender: *const c_char,
    pubkey: *const c_char,
    sig: *const c_char,
    delegation: *const c_char,
    canister_method: *const c_char,
}

impl From<EncodingResult<RequestMetadata>> for GetRequestMetadataResult {
    fn from(result: EncodingResult<RequestMetadata>) -> Self {
        match result {
            Ok(metadata) => {
                let (rtype, rid, sid, cm) = match metadata {
                    RequestMetadata::Call { request_id, sender_info, canister_method } => {
                        ("call".to_string(), Some(request_id), sender_info, canister_method)
                    }
                    RequestMetadata::ReadState { request_id, sender_info } => {
                        ("read_state".to_string(), request_id, sender_info, "".to_string())
                    }
                    RequestMetadata::Query { request_id, sender_info, canister_method } => {
                        ("query".to_string(), Some(request_id), sender_info, canister_method)
                    }
                };

                GetRequestMetadataResult {
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: match rid {
                        None => null(),
                        Some(rid) => str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid))
                    },
                    sender: str_to_ptr(sid.sender.to_text()),
                    pubkey: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.pubkey.unwrap_or_default())),
                    sig: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.sig.unwrap_or_default())),
                    delegation: str_to_ptr(delegation_to_string(sid.delegation)),
                    canister_method: str_to_ptr(cm),
                }
            }
            Err(err) => {
                GetRequestMetadataResult {
                    error_message: str_to_ptr(err.to_string()),
                    request_type: null(),
                    request_id: null(),
                    sender: null(),
                    pubkey: null(),
                    sig: null(),
                    delegation: null(),
                    canister_method: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct DecodeCanisterRequestResult {
    error_message: *const c_char,
    request_type: *const c_char,
    request_id: *const c_char,
    sender: *const c_char,
    pubkey: *const c_char,
    sig: *const c_char,
    delegation: *const c_char,
    decoded_request: *const c_char,
    canister_method: *const c_char,
}

impl DecodeCanisterRequestResult {
    pub fn error(err: String) -> Self {
        DecodeCanisterRequestResult {
            error_message: str_to_ptr(err),
            request_type: null(),
            request_id: null(),
            sender: null(),
            pubkey: null(),
            sig: null(),
            delegation: null(),
            decoded_request: null(),
            canister_method: null(),
        }
    }
}

impl From<EncodingResult<RequestInfo>> for DecodeCanisterRequestResult {
    fn from(result: EncodingResult<RequestInfo>) -> Self {
        match result {
            Ok(info) => {
                let (rtype, rid, sid, dreq, cm) = match info {
                    RequestInfo::Call { request_id, decoded_request, sender_info, canister_method } => {
                        ("call".to_string(), Some(request_id), sender_info, decoded_request, canister_method)
                    }
                    RequestInfo::ReadState { request_id, sender_info, decoded_request } => {
                        ("read_state".to_string(), request_id, sender_info, decoded_request, "".to_string())
                    }
                    RequestInfo::Query { request_id, sender_info, decoded_request, canister_method } => {
                        ("query".to_string(), Some(request_id), sender_info, decoded_request, canister_method)
                    }
                };

                DecodeCanisterRequestResult {
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: match rid {
                        None => null(),
                        Some(rid) => str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid))
                    },
                    sender: str_to_ptr(sid.sender.to_text()),
                    pubkey: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.pubkey.unwrap_or_default())),
                    sig: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.sig.unwrap_or_default())),
                    delegation: str_to_ptr(delegation_to_string(sid.delegation)),
                    decoded_request: str_to_ptr(dreq),
                    canister_method: str_to_ptr(cm),
                }
            }
            Err(err) => {
                DecodeCanisterRequestResult {
                    error_message: str_to_ptr(err.to_string()),
                    request_type: null(),
                    request_id: null(),
                    sender: null(),
                    pubkey: null(),
                    sig: null(),
                    delegation: null(),
                    decoded_request: null(),
                    canister_method: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct DecodeCanisterResponseResult {
    error_message: *const c_char,
    decoded_response: *const c_char,
}

impl DecodeCanisterResponseResult {
    pub fn error(err: String) -> Self {
        DecodeCanisterResponseResult {
            error_message: str_to_ptr(err),
            decoded_response: null(),
        }
    }
}

impl From<EncodingResult<String>> for DecodeCanisterResponseResult {
    fn from(result: EncodingResult<String>) -> Self {
        match result {
            Ok(decoded_request) => {
                DecodeCanisterResponseResult {
                    error_message: null(),
                    decoded_response: str_to_ptr(decoded_request),
                }
            }
            Err(err) => {
                DecodeCanisterResponseResult {
                    error_message: str_to_ptr(err.to_string()),
                    decoded_response: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct GenerateEd25519KeyResult {
    pub error_message: *const c_char,
    pub pem_encoded_key: *const c_char,
}

impl GenerateEd25519KeyResult {
    pub fn success(pem: String) -> Self {
        GenerateEd25519KeyResult {
            error_message: null(),
            pem_encoded_key: str_to_ptr(pem),
        }
    }

    pub fn error(err: String) -> Self {
        GenerateEd25519KeyResult {
            error_message: str_to_ptr(err),
            pem_encoded_key: null(),
        }
    }
}

#[repr(C)]
pub struct EncodeAndSignCanisterRequestResult {
    error_message: *const c_char,
    encoded_request: *const c_char,
}

impl EncodeAndSignCanisterRequestResult {
    pub fn error(err: String) -> Self {
        EncodeAndSignCanisterRequestResult {
            error_message: str_to_ptr(err),
            encoded_request: null(),
        }
    }
}

impl From<EncodingResult<Vec<u8>>> for EncodeAndSignCanisterRequestResult {
    fn from(result: EncodingResult<Vec<u8>>) -> Self {
        match result {
            Ok(encoded_request) => {
                EncodeAndSignCanisterRequestResult {
                    error_message: null(),
                    encoded_request: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&encoded_request)),
                }
            }
            Err(err) => {
                EncodeAndSignCanisterRequestResult {
                    error_message: str_to_ptr(err.to_string()),
                    encoded_request: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct InternetIdentityAddTentativePasskeyResult {
    error_message: *const c_char,
    code: *const c_char,
}

impl InternetIdentityAddTentativePasskeyResult {
    pub fn error(err: String) -> Self {
        InternetIdentityAddTentativePasskeyResult {
            error_message: str_to_ptr(err),
            code: null(),
        }
    }
}

impl From<InternetIdentityResult<String>> for InternetIdentityAddTentativePasskeyResult {
    fn from(result: InternetIdentityResult<String>) -> Self {
        match result {
            Ok(code) => {
                InternetIdentityAddTentativePasskeyResult {
                    error_message: null(),
                    code: str_to_ptr(code),
                }
            }
            Err(err) => {
                InternetIdentityAddTentativePasskeyResult::error(err.to_string())
            }
        }
    }
}

#[repr(C)]
pub struct InternetIdentityIsPasskeyRegisteredResult {
    error_message: *const c_char,
    is_passkey_registered: *const c_char,
}

impl InternetIdentityIsPasskeyRegisteredResult {
    pub fn error(err: String) -> Self {
        InternetIdentityIsPasskeyRegisteredResult {
            error_message: str_to_ptr(err),
            is_passkey_registered: null(),
        }
    }
}

impl From<InternetIdentityResult<bool>> for InternetIdentityIsPasskeyRegisteredResult {
    fn from(result: InternetIdentityResult<bool>) -> Self {
        match result {
            Ok(is_passkey_registered) => {
                InternetIdentityIsPasskeyRegisteredResult {
                    error_message: null(),
                    is_passkey_registered: if is_passkey_registered { str_to_ptr("true".to_string()) } else { str_to_ptr("false".to_string()) },
                }
            }
            Err(err) => {
                InternetIdentityIsPasskeyRegisteredResult::error(err.to_string())
            }
        }
    }
}

#[repr(C)]
pub struct InternetIdentityGetPrincipalResult {
    error_message: *const c_char,
    principal: *const c_char,
}

impl InternetIdentityGetPrincipalResult {
    pub fn error(err: String) -> Self {
        InternetIdentityGetPrincipalResult {
            error_message: str_to_ptr(err),
            principal: null(),
        }
    }
}

impl From<InternetIdentityResult<Principal>> for InternetIdentityGetPrincipalResult {
    fn from(result: InternetIdentityResult<Principal>) -> Self {
        match result {
            Ok(principal) => {
                InternetIdentityGetPrincipalResult {
                    error_message: null(),
                    principal: str_to_ptr(principal.to_text()),
                }
            }
            Err(err) => {
                InternetIdentityGetPrincipalResult::error(err.to_string())
            }
        }
    }
}

#[repr(C)]
pub struct InternetIdentityGetDelegationResult {
    pub error_message: *const c_char,
    pub from_pubkey: *const c_char,
    pub delegation: *const c_char,
}

impl InternetIdentityGetDelegationResult {
    pub fn error(err: String) -> Self {
        InternetIdentityGetDelegationResult {
            error_message: str_to_ptr(err),
            from_pubkey: null(),
            delegation: null(),
        }
    }
}

impl From<InternetIdentityResult<DelegationInfo>> for InternetIdentityGetDelegationResult {
    fn from(result: InternetIdentityResult<DelegationInfo>) -> Self {
        match result {
            Ok(delegation_info) => {
                InternetIdentityGetDelegationResult {
                    error_message: null(),
                    from_pubkey: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(delegation_info.from_pubkey)),
                    delegation: str_to_ptr(delegation_to_string(delegation_info.delegation_chain)),
                }
            }
            Err(err) => {
                InternetIdentityGetDelegationResult::error(err.to_string())
            }
        }
    }
}
use std::os::raw::c_char;
use std::ptr::null;

use base64::Engine;

use crate::canister_lookup::model::LookupResult;
use crate::encode::EncodingResult;
use crate::encode::model::{RequestInfo, RequestMetadata};
use crate::jna::{delegation_to_string, str_to_ptr};

#[repr(C)]
pub struct DiscoverCanisterInterfaceResult {
    is_successful: bool,
    error_message: *const c_char,
    canister_interface: *const c_char,
}

impl From<LookupResult<Option<String>>> for DiscoverCanisterInterfaceResult {
    fn from(result: LookupResult<Option<String>>) -> Self {
        match result {
            Ok(interface_opt) => {
                DiscoverCanisterInterfaceResult {
                    is_successful: true,
                    error_message: null(),
                    canister_interface: str_to_ptr(interface_opt.unwrap_or("".to_string())),
                }
            }
            Err(err) => {
                DiscoverCanisterInterfaceResult {
                    is_successful: false,
                    error_message: str_to_ptr(err.to_string()),
                    canister_interface: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct GetRequestMetadataResult {
    is_successful: bool,
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
                        ("call".to_string(), request_id, sender_info, canister_method)
                    }
                    RequestMetadata::ReadState { request_id, sender_info } => {
                        ("read_state".to_string(), request_id, sender_info, "".to_string())
                    }
                    RequestMetadata::Query { request_id, sender_info, canister_method } => {
                        ("query".to_string(), request_id, sender_info, canister_method)
                    }
                };

                GetRequestMetadataResult {
                    is_successful: true,
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid)),
                    sender: str_to_ptr(sid.sender.to_text()),
                    pubkey: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.pubkey.unwrap_or_default())),
                    sig: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&sid.sig.unwrap_or_default())),
                    delegation: str_to_ptr(delegation_to_string(sid.delegation)),
                    canister_method: str_to_ptr(cm),
                }
            }
            Err(err) => {
                GetRequestMetadataResult {
                    is_successful: false,
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
    is_successful: bool,
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

impl From<EncodingResult<RequestInfo>> for DecodeCanisterRequestResult {
    fn from(result: EncodingResult<RequestInfo>) -> Self {
        match result {
            Ok(info) => {
                let (rtype, rid, sid, dreq, cm) = match info {
                    RequestInfo::Call { request_id, decoded_request, sender_info, canister_method } => {
                        ("call".to_string(), request_id, sender_info, decoded_request, canister_method)
                    }
                    RequestInfo::ReadState { request_id, sender_info, decoded_request } => {
                        ("read_state".to_string(), request_id, sender_info, decoded_request, "".to_string())
                    }
                    RequestInfo::Query { request_id, sender_info, decoded_request, canister_method } => {
                        ("query".to_string(), request_id, sender_info, decoded_request, canister_method)
                    }
                };

                DecodeCanisterRequestResult {
                    is_successful: true,
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid)),
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
                    is_successful: false,
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
    is_successful: bool,
    error_message: *const c_char,
    decoded_response: *const c_char,
}

impl From<EncodingResult<String>> for DecodeCanisterResponseResult {
    fn from(result: EncodingResult<String>) -> Self {
        match result {
            Ok(decoded_request) => {
                DecodeCanisterResponseResult {
                    is_successful: true,
                    error_message: null(),
                    decoded_response: str_to_ptr(decoded_request),
                }
            }
            Err(err) => {
                DecodeCanisterResponseResult {
                    is_successful: false,
                    error_message: str_to_ptr(err.to_string()),
                    decoded_response: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct GenerateEd25519KeyResult {
    is_successful: bool,
    error_message: *const c_char,
    pem_encoded_key: *const c_char,
}

impl GenerateEd25519KeyResult {
    pub fn success(pem: String) -> Self {
        GenerateEd25519KeyResult {
            is_successful: true,
            error_message: null(),
            pem_encoded_key: str_to_ptr(pem),
        }
    }

    pub fn error(err: String) -> Self {
        GenerateEd25519KeyResult {
            is_successful: false,
            error_message: str_to_ptr(err),
            pem_encoded_key: null(),
        }
    }
}

#[repr(C)]
pub struct EncodeAndSignCanisterRequestResult {
    is_successful: bool,
    error_message: *const c_char,
    encoded_request: *const c_char,
}

impl EncodeAndSignCanisterRequestResult {
    pub fn error(err: String) -> Self {
        EncodeAndSignCanisterRequestResult {
            is_successful: false,
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
                    is_successful: true,
                    error_message: null(),
                    encoded_request: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&encoded_request)),
                }
            }
            Err(err) => {
                EncodeAndSignCanisterRequestResult {
                    is_successful: false,
                    error_message: str_to_ptr(err.to_string()),
                    encoded_request: null(),
                }
            }
        }
    }
}

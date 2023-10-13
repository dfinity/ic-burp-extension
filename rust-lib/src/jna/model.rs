use std::os::raw::c_char;
use std::ptr::null;

use base64::Engine;

use crate::canister_lookup::model::LookupResult;
use crate::encode::EncodingResult;
use crate::encode::model::{RequestInfo, RequestMetadata};
use crate::jna::str_to_ptr;

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
    canister_method: *const c_char,
}

impl From<EncodingResult<RequestMetadata>> for GetRequestMetadataResult {
    fn from(result: EncodingResult<RequestMetadata>) -> Self {
        match result {
            Ok(metadata) => {
                let (rtype, rid, cm) = match metadata {
                    RequestMetadata::Call { request_id, canister_method } => {
                        ("call".to_string(), request_id, canister_method)
                    }
                    RequestMetadata::ReadState { request_id } => {
                        ("read_state".to_string(), request_id, "".to_string())
                    }
                    RequestMetadata::Query { request_id, canister_method } => {
                        ("query".to_string(), request_id, canister_method)
                    }
                };
                GetRequestMetadataResult {
                    is_successful: true,
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid)),
                    canister_method: str_to_ptr(cm),
                }
            }
            Err(err) => {
                GetRequestMetadataResult {
                    is_successful: false,
                    error_message: str_to_ptr(err.to_string()),
                    request_type: null(),
                    request_id: null(),
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
    decoded_request: *const c_char,
    canister_method: *const c_char,
}

impl From<EncodingResult<RequestInfo>> for DecodeCanisterRequestResult {
    fn from(result: EncodingResult<RequestInfo>) -> Self {
        match result {
            Ok(info) => {
                let (rtype, rid, dreq, cm) = match info {
                    RequestInfo::Call { request_id, decoded_request, canister_method } => {
                        ("call".to_string(), request_id, decoded_request, canister_method)
                    }
                    RequestInfo::ReadState { request_id, decoded_request } => {
                        ("read_state".to_string(), request_id, decoded_request, "".to_string())
                    }
                    RequestInfo::Query { request_id, decoded_request, canister_method } => {
                        ("query".to_string(), request_id, decoded_request, canister_method)
                    }
                };

                DecodeCanisterRequestResult {
                    is_successful: true,
                    error_message: null(),
                    request_type: str_to_ptr(rtype),
                    request_id: str_to_ptr(base64::engine::general_purpose::STANDARD_NO_PAD.encode(&rid)),
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
                    decoded_request: null(),
                    canister_method: null(),
                }
            }
        }
    }
}

#[repr(C)]
pub struct CanisterInterfaceInfo {
    pub canister_interface: *const c_char,
    pub canister_method: *const c_char,
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

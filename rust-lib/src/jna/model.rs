use std::os::raw::c_char;
use std::ptr::null;
use base64::Engine;
use crate::encode::EncodingResult;
use crate::encode::model::RequestInfo;
use crate::jna::str_to_ptr;

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
            },
            Err(err) => {
                DecodeCanisterRequestResult {
                    is_successful: false,
                    error_message: str_to_ptr(err.to_string()),
                    request_type: null(),
                    request_id: null(),
                    decoded_request: null(),
                    canister_method: null(),
                }
            },
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

use std::ffi::{CStr, CString};
use std::mem;
use std::os::raw::c_char;
use std::ptr::null;

use model::DecodeCanisterRequestResult;

use crate::{canister_lookup, encode};
use crate::jna::model::{CanisterInterfaceInfo, DecodeCanisterResponseResult, DiscoverCanisterInterfaceResult, GetRequestMetadataResult};

mod model;

#[no_mangle]
#[tokio::main]
pub async extern fn discover_canister_interface(canister_id: *const c_char) -> DiscoverCanisterInterfaceResult {
    canister_lookup::discover_canister_interface(to_string(canister_id), None).await.into()
}

#[no_mangle]
pub extern fn get_request_metadata(encoded_cbor_request: *const u8, encoded_cbor_request_size: usize) -> GetRequestMetadataResult {
    encode::get_request_metadata(to_vec(encoded_cbor_request, encoded_cbor_request_size)).into()
}

#[no_mangle]
pub extern fn decode_canister_request(encoded_cbor_request: *const u8, encoded_cbor_request_size: usize, canister_interface_opt: *const c_char) -> DecodeCanisterRequestResult {
    let canister_interface = if canister_interface_opt == null() {
        None
    } else {
        Some(to_string(canister_interface_opt))
    };

    encode::decode_canister_request(to_vec(encoded_cbor_request, encoded_cbor_request_size), canister_interface).into()
}

#[no_mangle]
pub extern fn decode_canister_response(encoded_cbor_response: *const u8, encoded_cbor_response_size: usize, canister_interface_info_opt: &CanisterInterfaceInfo) -> DecodeCanisterResponseResult {
    let canister_interface_info = if canister_interface_info_opt.canister_interface == null() || canister_interface_info_opt.canister_method == null() {
        None
    } else {
        Some(encode::model::CanisterInterfaceInfo {
            canister_interface: to_string(canister_interface_info_opt.canister_interface),
            canister_method: to_string(canister_interface_info_opt.canister_method),
        })
    };
    encode::decode_canister_response(to_vec(encoded_cbor_response, encoded_cbor_response_size), canister_interface_info).into()
}

/// Convert a native string to a Rust string
fn to_string(pointer: *const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    std::str::from_utf8(slice).unwrap().to_string()
}

/// Convert a Rust string to a native string
fn str_to_ptr(string: String) -> *const c_char {
    if string.len() == 0 {
        return null();
    }
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.
    mem::forget(cs);
    ptr
}

fn to_vec(buf: *const u8, size: usize) -> Vec<u8> {
    let mut v = Vec::with_capacity(size);
    unsafe {
        for i in 0..size {
            v.push(buf.add(i).read());
        }
    }
    v
}

fn vec_to_ptr(vec: Vec<u8>) -> (*const u8, usize) {
    (vec.as_ptr(), vec.len())
}

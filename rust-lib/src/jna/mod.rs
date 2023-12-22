use std::any::Any;
use std::ffi::{CStr, CString};
use std::mem;
use std::num::ParseIntError;
use std::os::raw::c_char;
use std::ptr::null;
use std::sync::Arc;

use base64::Engine;
use candid::Principal;
use ic_agent::Identity;
use ic_agent::identity::{AnonymousIdentity, BasicIdentity, DelegatedIdentity, Secp256k1Identity};
use ic_transport_types::SignedDelegation;
use ring::signature::Ed25519KeyPair;

use model::DecodeCanisterRequestResult;

use crate::{canister_lookup, encode};
use crate::encode::model::RequestSenderDelegation;
use crate::jna::model::{DecodeCanisterResponseResult, DiscoverCanisterInterfaceResult, EncodeAndSignCanisterRequestResult, GenerateEd25519KeyResult, GetRequestMetadataResult};

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
pub extern fn decode_canister_response(encoded_cbor_response: *const u8, encoded_cbor_response_size: usize, canister_interface_opt: *const c_char, canister_method_opt: *const c_char) -> DecodeCanisterResponseResult {
    let canister_interface_info = if canister_interface_opt == null() || canister_method_opt == null() {
        None
    } else {
        Some(encode::model::CanisterInterfaceInfo {
            canister_interface: to_string(canister_interface_opt),
            canister_method: to_string(canister_method_opt),
        })
    };
    encode::decode_canister_response(to_vec(encoded_cbor_response, encoded_cbor_response_size), canister_interface_info).into()
}

#[no_mangle]
pub extern fn generate_ed25519_key() -> GenerateEd25519KeyResult {
    return match Ed25519KeyPair::generate_pkcs8(&ring::rand::SystemRandom::new()) {
        Ok(pem_doc) => {
            let pem_enc = base64::engine::general_purpose::STANDARD.encode(pem_doc.as_ref());
            let pem_export = format!("-----BEGIN PRIVATE KEY-----\n{}\n-----END PRIVATE KEY-----", pem_enc);
            GenerateEd25519KeyResult::success(pem_export)
        }
        Err(e) => {
            GenerateEd25519KeyResult::error(e.to_string())
        }
    };
}

#[no_mangle]
pub extern fn encode_and_sign_canister_request(decoded_request: *const c_char, canister_interface_opt: *const c_char, identity_type: *const c_char, identity_pem_opt: *const c_char, identity_delegation_from_pubkey_opt: *const c_char, identity_delegation_chain_opt: *const c_char) -> EncodeAndSignCanisterRequestResult {
    let req = to_string(decoded_request);
    let canister_interface = if canister_interface_opt == null() {
        None
    } else {
        Some(to_string(canister_interface_opt))
    };
    return match create_identity(identity_type, identity_pem_opt, identity_delegation_from_pubkey_opt, identity_delegation_chain_opt) {
        Ok(identity) => encode::encode_and_sign_canister_request(req, canister_interface, identity).into(),
        Err(e) => EncodeAndSignCanisterRequestResult::error(e),
    };
}

fn create_identity(identity_type: *const c_char, identity_pem_opt: *const c_char, identity_delegation_from_pubkey_opt: *const c_char, identity_delegation_chain_opt: *const c_char) -> Result<Arc<dyn Identity>, String> {
    let typ = to_string(identity_type);
    return match typ.to_uppercase().as_str() {
        "ANONYMOUS" => Ok(Arc::new(AnonymousIdentity {})),
        "ED25519" => {
            let identity = BasicIdentity::from_pem(to_string(identity_pem_opt).as_bytes()).map_err(|x| x.to_string())?;
            if identity_delegation_from_pubkey_opt == null() || identity_delegation_chain_opt == null() {
                Ok(Arc::new(identity))
            } else {
                let from_key = base64::engine::general_purpose::STANDARD_NO_PAD.decode(to_string(identity_delegation_from_pubkey_opt)).map_err(|e| e.to_string())?;
                let del_identity = DelegatedIdentity::new(from_key, Box::new(identity), string_to_delegation(to_string(identity_delegation_chain_opt))?);
                Ok(Arc::new(del_identity))
            }
        }
        "SECP256K1" => {
            let identity = Secp256k1Identity::from_pem(to_string(identity_pem_opt).as_bytes()).map_err(|x| x.to_string())?;
            if identity_delegation_from_pubkey_opt == null() || identity_delegation_chain_opt == null() {
                Ok(Arc::new(identity))
            } else {
                let from_key = base64::engine::general_purpose::STANDARD_NO_PAD.decode(to_string(identity_delegation_from_pubkey_opt)).map_err(|e| e.to_string())?;
                let del_identity = DelegatedIdentity::new(from_key, Box::new(identity), string_to_delegation(to_string(identity_delegation_chain_opt))?);
                Ok(Arc::new(del_identity))
            }
        }
        _ => Err(format!("unknown identity_type: {typ}")),
    };
}

/// Convert a delegation list to a string
fn delegation_to_string(delegations: Vec<RequestSenderDelegation>) -> String {
    let mut res = String::new();
    res.push_str(&*delegations.len().to_string());
    for del in delegations {
        res.push_str(";");
        res.push_str(&*base64::engine::general_purpose::STANDARD_NO_PAD.encode(del.pubkey));
        res.push_str(":");
        res.push_str(&*del.expiration.to_string());
        res.push_str(":");
        res.push_str(&*del.targets.len().to_string());
        for t in del.targets {
            res.push_str(",");
            res.push_str(&*t.to_text());
        }
        res.push_str(":");

        res.push_str(&*base64::engine::general_purpose::STANDARD_NO_PAD.encode(del.signature));
    }
    res
}

/// Convert a string into a delegation list
fn string_to_delegation(delegations: String) -> Result<Vec<SignedDelegation>, String> {
    let mut res = Vec::new();
    for del in delegations.split(";").into_iter().skip(1) {
        let fields: Vec<&str> = del.split(":").into_iter().collect();
        if fields.len() != 4 {
            return Err(format!("unexpected number of fields in delegation: {0}", fields.len()));
        }
        let pubkey = base64::engine::general_purpose::STANDARD_NO_PAD.decode(fields[0]).map_err(|e| e.to_string())?;
        let expiration: u64 = fields[1].parse().map_err(|e: ParseIntError| e.to_string())?;
        let mut targets = Vec::new();
        for target in fields[2].split(",").into_iter().skip(1) {
            targets.push(Principal::from_text(target).map_err(|e| e.to_string())?);
        }
        let signature = base64::engine::general_purpose::STANDARD_NO_PAD.decode(fields[3]).map_err(|e| e.to_string())?;
        res.push(RequestSenderDelegation {
            pubkey,
            expiration,
            targets,
            signature,
        }.into())
    }
    Ok(res)
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

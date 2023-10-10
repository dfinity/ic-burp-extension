use std::borrow::Cow;
use std::ffi::CString;
use cbor::Decoder;
use rustc_serialize::json::ToJson;
use std::os::raw::c_char;
use std::{fmt, mem};
use candid::{IDLArgs, pretty_parse, Principal};
use candid::utils::CandidSource;
use hex;
use ic_agent::to_request_id;
use ic_certification::Certificate;
use ic_certification::hash_tree::{HashTree, HashTreeNode};
use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde_cbor;
use serde_repr::{Deserialize_repr, Serialize_repr};
use thiserror::Error;

pub mod encode;
mod jna;

//TODO: clean up this file

#[no_mangle]
pub extern fn cbor_decode(buf: *mut u8, size: usize) -> *mut c_char {
    let data = unsafe { Vec::from_raw_parts(buf, size, size) };
    let mut d = Decoder::from_bytes(data.as_slice());
    let cbor = d.items().next().unwrap().unwrap();
    mem::forget(data);
    unsafe { CString::from_vec_unchecked(cbor.to_json().to_string().into_bytes()) }.into_raw()
}





#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct Envelope<'a> {
    /// The data that is signed by the caller.
    pub content: Cow<'a, EnvelopeContent>,
    /// The public key of the self-signing principal this request is from.
    #[serde(default, skip_serializing_if = "Option::is_none", with = "serde_bytes")]
    pub sender_pubkey: Option<Vec<u8>>,
    /// A cryptographic signature authorizing the request. Not necessarily made by `sender_pubkey`; when delegations are involved,
    /// `sender_sig` is the tail of the delegation chain, and `sender_pubkey` is the head.
    #[serde(default, skip_serializing_if = "Option::is_none", with = "serde_bytes")]
    pub sender_sig: Option<Vec<u8>>,
    /// The chain of delegations connecting `sender_pubkey` to `sender_sig`, and in that order.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub sender_delegation: Option<Vec<SignedDelegation>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "request_type", rename_all = "snake_case")]
pub enum EnvelopeContent {
    /// A replicated call to a canister method, whether update or query.
    Call {
        /// A random series of bytes to uniquely identify this message.
        #[serde(default, skip_serializing_if = "Option::is_none", with = "serde_bytes")]
        nonce: Option<Vec<u8>>,
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// The ID of the canister to be called.
        canister_id: Principal,
        /// The name of the canister method to be called.
        method_name: String,
        /// The argument to pass to the canister method.
        #[serde(with = "serde_bytes")]
        arg: Vec<u8>,
    },
    /// A request for information from the [IC state tree](https://internetcomputer.org/docs/current/references/ic-interface-spec#state-tree).
    ReadState {
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// A list of paths within the state tree to fetch.

        paths: Vec<Vec<PathLabel>>,
    },
    /// An unreplicated call to a canister query method.
    Query {
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// The ID of the canister to be called.
        canister_id: Principal,
        /// The name of the canister method to be called.
        method_name: String,
        /// The argument to pass to the canister method.
        #[serde(with = "serde_bytes")]
        arg: Vec<u8>,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SignedDelegation {
    /// The signed delegation.
    pub delegation: Delegation,
    /// The signature for the delegation.
    #[serde(with = "serde_bytes")]
    pub signature: Vec<u8>,
}
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Delegation {
    /// The delegated-to key.
    #[serde(with = "serde_bytes")]
    pub pubkey: Vec<u8>,
    /// A nanosecond timestamp after which this delegation is no longer valid.
    pub expiration: u64,
    /// If present, this delegation only applies to requests sent to one of these canisters.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub targets: Option<Vec<Principal>>,
    /// If present, this delegation only applies to requests originating from one of these principals.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub senders: Option<Vec<Principal>>,
}

const IC_REQUEST_DELEGATION_DOMAIN_SEPARATOR: &[u8] = b"\x1Aic-request-auth-delegation";

impl Delegation {
    /// Returns the signable form of the delegation, by running it through [`to_request_id`]
    /// and prepending `\x1Aic-request-auth-delegation` to the result.
    pub fn signable(&self) -> Vec<u8> {
        let hash = to_request_id(self).unwrap();
        let mut bytes = Vec::with_capacity(59);
        bytes.extend_from_slice(IC_REQUEST_DELEGATION_DOMAIN_SEPARATOR);
        bytes.extend_from_slice(hash.as_slice());
        bytes
    }
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct EnvelopePretty<'a> {
    pub content: Cow<'a, EnvelopeContentPretty>,
    #[serde(default)]
    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(with = "serde_bytes")]
    pub sender_pubkey: Option<Vec<u8>>,
    #[serde(default)]
    #[serde(skip_serializing_if = "Option::is_none")]
    #[serde(with = "serde_bytes")]
    pub sender_sig: Option<Vec<u8>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub sender_delegation: Option<Vec<SignedDelegation>>,
}

impl Into<EnvelopePretty<'_>> for Envelope<'_> {
    fn into(self) -> EnvelopePretty<'static> {
        EnvelopePretty {
            content: Cow::Owned(self.content.into_owned().into()),
            sender_pubkey: self.sender_pubkey,
            sender_sig: self.sender_sig,
            sender_delegation: self.sender_delegation
        }
    }
}

impl Into<Envelope<'_>> for EnvelopePretty<'_> {
    fn into(self) -> Envelope<'static> {
        Envelope {
            content: Cow::Owned(self.content.into_owned().into()),
            sender_pubkey: self.sender_pubkey,
            sender_sig: self.sender_sig,
            sender_delegation: self.sender_delegation,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "request_type", rename_all = "snake_case")]
pub enum EnvelopeContentPretty {
    /// A replicated call to a canister method, whether update or query.
    Call {
        /// A random series of bytes to uniquely identify this message.
        #[serde(default, skip_serializing_if = "Option::is_none", with = "serde_bytes")]
        nonce: Option<Vec<u8>>,
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// The ID of the canister to be called.
        canister_id: Principal,
        /// The name of the canister method to be called.
        method_name: String,
        /// The argument to pass to the canister method.
        arg: String,
    },
    /// A request for information from the [IC state tree](https://internetcomputer.org/docs/current/references/ic-interface-spec#state-tree).
    ReadState {
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// A list of paths within the state tree to fetch.
        paths: Vec<Vec<PathLabel>>,
    },
    /// An unreplicated call to a canister query method.
    Query {
        /// A nanosecond timestamp after which this request is no longer valid.
        ingress_expiry: u64,
        /// The principal that is sending this request.
        sender: Principal,
        /// The ID of the canister to be called.
        canister_id: Principal,
        /// The name of the canister method to be called.
        method_name: String,
        /// The argument to pass to the canister method.
        arg: String,
    },
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(tag = "status")]
pub enum QueryResponse {
    #[serde(rename = "replied")]
    Replied { reply: CallReply, signatures: Vec<NodeSignature> },
    #[serde(rename = "rejected")]
    Rejected(RejectResponse),
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct CallReply {
    #[serde(with = "serde_bytes")]
    pub arg: Vec<u8>,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(tag = "status")]
pub enum QueryResponsePretty {
    #[serde(rename = "replied")]
    Replied { reply: CallReplyPretty, signatures: Vec<NodeSignature> },
    #[serde(rename = "rejected")]
    Rejected(RejectResponse),
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct CallReplyPretty {
    pub arg: String,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct NodeSignature {
    pub timestamp: u64,
    #[serde(with = "serde_bytes")]
    pub signature: Vec<u8>,
    #[serde(with = "serde_bytes")]
    pub identity: Vec<u8>,
}

impl Into<QueryResponsePretty> for QueryResponse {
    fn into(self) -> QueryResponsePretty {
        match self {
            QueryResponse::Replied { signatures, .. } => { QueryResponsePretty::Replied { reply: CallReplyPretty { arg: "ARG_PLACEHOLDER".parse().unwrap() }, signatures}}
            QueryResponse::Rejected(x) => { QueryResponsePretty::Rejected(x) }
        }
    }
}

impl Into<QueryResponse> for QueryResponsePretty {
    fn into(self) -> QueryResponse {
        match self {
            QueryResponsePretty::Replied { signatures, .. } => { QueryResponse::Replied { reply: CallReply { arg: vec![] }, signatures}}
            QueryResponsePretty::Rejected(x) => { QueryResponse::Rejected(x) }
        }
    }
}

/// An IC execution error received from the replica.
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct RejectResponse {
    /// The [reject code](https://internetcomputer.org/docs/current/references/ic-interface-spec#reject-codes) returned by the replica.
    pub reject_code: RejectCode,
    /// The rejection message.
    pub reject_message: String,
    /// The optional [error code](https://internetcomputer.org/docs/current/references/ic-interface-spec#error-codes) returned by the replica.
    #[serde(default)]
    pub error_code: Option<String>,
    pub signatures: Vec<NodeSignature>
}

/// See the [interface spec](https://internetcomputer.org/docs/current/references/ic-interface-spec#reject-codes).
#[derive(
Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize_repr, Deserialize_repr, Ord, PartialOrd,
)]
#[repr(u8)]
pub enum RejectCode {
    /// Fatal system error, retry unlikely to be useful
    SysFatal = 1,
    /// Transient system error, retry might be possible.
    SysTransient = 2,
    /// Invalid destination (e.g. canister/account does not exist)
    DestinationInvalid = 3,
    /// Explicit reject by the canister.
    CanisterReject = 4,
    /// Canister error (e.g., trap, no response)
    CanisterError = 5,
}

impl TryFrom<u64> for RejectCode {
    type Error = LibError;

    fn try_from(value: u64) -> Result<Self, LibError> {
        match value {
            1 => Ok(RejectCode::SysFatal),
            2 => Ok(RejectCode::SysTransient),
            3 => Ok(RejectCode::DestinationInvalid),
            4 => Ok(RejectCode::CanisterReject),
            5 => Ok(RejectCode::CanisterError),
            _ => Err(LibError::MessageError(format!(
                "Received an invalid reject code {}",
                value
            ))),
        }
    }
}

#[derive(Error, Debug)]
pub enum LibError {
    /// A string error occurred in an external tool.
    #[error("A tool returned a string message error: {0}")]
    MessageError(String),
}

#[derive(Debug, Clone, Deserialize, Serialize, PartialEq, Eq)]
pub struct ReadStateResponse {
    #[serde(with = "serde_bytes")]
    pub certificate: Vec<u8>,
}

#[derive(Debug, Clone, Deserialize, Serialize, PartialEq, Eq)]
pub struct ReadStateResponsePretty {
    pub certificate: Certificate,
}

impl Into<ReadStateResponsePretty> for ReadStateResponse {
    fn into(self) -> ReadStateResponsePretty {
        ReadStateResponsePretty {
            certificate: serde_cbor::from_slice(&self.certificate).unwrap()
        }
    }
}

impl Into<ReadStateResponse> for ReadStateResponsePretty {
    fn into(self) -> ReadStateResponse {
        let mut serialized_bytes = Vec::new();
        let mut serializer = serde_cbor::Serializer::new(&mut serialized_bytes);
        serializer.self_describe().unwrap();
        self.certificate.serialize(&mut serializer).unwrap();

        ReadStateResponse {
            certificate: serialized_bytes
        }
    }
}


#[derive(Clone, Hash, Ord, PartialOrd, Eq, PartialEq)]
pub struct PathLabel(Vec<u8>);

impl PathLabel {
    pub fn as_bytes(&self) -> &[u8] {
        self.0.as_ref()
    }

    fn write_hex(&self, f: &mut impl fmt::Write) -> fmt::Result {
        self.as_bytes()
            .iter()
            .try_for_each(|b| write!(f, "{:02X}", b))
    }

    fn hex_len(&self) -> usize {
        self.as_bytes().len() * 2
    }
}

impl fmt::Debug for PathLabel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> std::fmt::Result {
        self.write_hex(f)
    }
}

impl Serialize for PathLabel {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        /*if serializer.is_human_readable() {
            let mut s = String::with_capacity(self.hex_len());
            self.write_hex(&mut s).unwrap();
            s.serialize(serializer)
        } else {*/
            serializer.serialize_bytes(self.0.as_ref())
        //}
    }
}
impl<'de> Deserialize<'de> for PathLabel
{
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
        where
            D: Deserializer<'de>,
    {
        serde_bytes::deserialize(deserializer).map(Self)
        /*let is_human_readable = deserializer.is_human_readable();
        let x = serde_bytes::deserialize(deserializer).map(Self);
        if !is_human_readable || !x.is_ok() {
            return x;
        }
        let src = x.unwrap();
        let mut dst = Vec::new();
        let mut i = 0usize;
        while i < src.0.len() {
            let mut b = 0u8;
            for j in 0..2usize {
                let cur = src.0[i+j] & 0x0f;
                b |= cur<<(4-j*4);
            }
            dst.push(b);
            i += 2;
        }
        Ok(PathLabel(dst))*/
    }
}

impl Into<EnvelopeContentPretty> for EnvelopeContent {
    fn into(self) -> EnvelopeContentPretty {
        match self {
            EnvelopeContent::Call { nonce, ingress_expiry, sender, canister_id, method_name, arg } => {
                EnvelopeContentPretty::Call {
                    nonce,
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg: "ARG_PLACEHOLDER".parse().unwrap(),
                }
            },
            EnvelopeContent::Query { ingress_expiry, sender, canister_id, method_name, arg } => {
                EnvelopeContentPretty::Query {
                    ingress_expiry: ingress_expiry,
                    sender: sender,
                    canister_id: canister_id,
                    method_name: method_name,
                    arg: "ARG_PLACEHOLDER".parse().unwrap(),
                }
            },
            EnvelopeContent::ReadState { ingress_expiry, sender, paths } => {
                EnvelopeContentPretty::ReadState {
                    ingress_expiry,
                    sender,
                    paths,
                }
            }
        }
    }
}

impl Into<EnvelopeContent> for EnvelopeContentPretty {
    fn into(self) -> EnvelopeContent {
        match self {
            EnvelopeContentPretty::Call { nonce, ingress_expiry, sender, canister_id, method_name, arg } => {
                EnvelopeContent::Call {
                    nonce,
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg: vec![],
                }
            },
            EnvelopeContentPretty::Query { ingress_expiry, sender, canister_id, method_name, arg } => {
                EnvelopeContent::Query {
                    ingress_expiry: ingress_expiry,
                    sender: sender,
                    canister_id: canister_id,
                    method_name: method_name,
                    arg: vec![],
                }
            },
            EnvelopeContentPretty::ReadState { ingress_expiry, sender, paths } => {
                EnvelopeContent::ReadState {
                    ingress_expiry,
                    sender,
                    paths,
                }
            }
        }
    }
}

#[derive(Clone)]
enum MessageType {
    REQUEST,
    RESPONSE,
}

#[derive(Clone)]
struct IdlConfig {
    idl: String,
    direction: MessageType,
    method: String,
}

fn vec_to_idl_arg_factory(arg: Vec<u8>, idl: &Option<IdlConfig>) -> IDLArgs {
    match idl {
        None => {IDLArgs::from_bytes(&arg).unwrap()}
        Some(config) => {
            let idl = CandidSource::Text(&*config.idl);
            let (type_env, actor) = idl.load().unwrap();
            let actor = actor.unwrap();
            let func = type_env.get_method(&actor, &*config.method).unwrap();
            let types = match config.direction {
                MessageType::REQUEST => {(&func.args).clone()}
                MessageType::RESPONSE => {(&func.rets).clone()}
            };
            IDLArgs::from_bytes_with_types(&arg, &type_env, &types).unwrap()
        }
    }
}

fn idl_arg_to_vec_factory(arg: IDLArgs, idl: &Option<IdlConfig>) -> Vec<u8> {
    match idl {
        None => {arg.to_bytes().unwrap()}
        Some(config) => {
            let idl = CandidSource::Text(&*config.idl);
            let (type_env, actor) = idl.load().unwrap();
            let actor = actor.unwrap();
            let func = type_env.get_method(&actor, &*config.method).unwrap();
            let types = match config.direction {
                MessageType::REQUEST => {(&func.args).clone()}
                MessageType::RESPONSE => {(&func.rets).clone()}
            };
            arg.to_bytes_with_types(&type_env, &types).unwrap()
        }
    }
}

fn find_candid_in_tree(root: HashTreeNode<Vec<u8>>) -> Vec<Vec<u8>> {
    match root {
        HashTreeNode::Empty() => { vec![] }
        HashTreeNode::Fork(next) => {
            let foo = *next;
            let mut left = find_candid_in_tree(foo.0);
            let mut right = find_candid_in_tree(foo.1);
            left.append(&mut right);
            left
        }
        HashTreeNode::Labeled(_, next) => {
            find_candid_in_tree(*next)
        }
        HashTreeNode::Leaf(x) => {
            if x[0..4] == [0x44, 0x49, 0x44, 0x4C] {
                vec![x]
            } else {
                vec![]
            }
        }
        HashTreeNode::Pruned(_) => { vec![] }
    }
}

fn print_cbor_decode_readstate_response(cbor: String, idl: Option<IdlConfig>) {
    let res = hex::decode(cbor).unwrap();
    let response: ReadStateResponse = serde_cbor::from_slice(&res).unwrap();
    let response_pretty: ReadStateResponsePretty = response.into();
    let mut response_json = serde_json::to_string(&response_pretty).unwrap();
    let finding = find_candid_in_tree(HashTreeNode::from(response_pretty.certificate.tree));
    for candid in finding {
        println!("{}", hex::encode(&candid));
        let candid_json = serde_json::to_string(&candid).unwrap();
        let candid_pretty = vec_to_idl_arg_factory(candid, &idl).to_string();
        response_json = response_json.replace(&*candid_json, &*candid_pretty);
    }
    println!("{}", response_json);
}

fn print_cbor_decode_encode_query_response(cbor: String, idl: Option<IdlConfig>) {
    let res = hex::decode(cbor).unwrap();
    let query_response : QueryResponse = serde_cbor::from_slice(&res).unwrap();
    let pretty: QueryResponsePretty = query_response.clone().into();
    let query_json = serde_json::to_string_pretty(&pretty).unwrap();

    let mut has_candid;
    let mut candid_pretty = String::from("");
    let query_json = match query_response.clone() {
        QueryResponse::Replied {reply: x, ..} => {
            has_candid = true;
            candid_pretty = vec_to_idl_arg_factory(x.arg, &idl).to_string();
            query_json.replace("ARG_PLACEHOLDER", &*candid_pretty)
        }
        QueryResponse::Rejected(_) => {
            has_candid = false;
            query_json
        }
    };
    println!("{}", query_json);

    // encode
    let response: QueryResponse = if has_candid {
        let a = query_json.find("\"(");
        let b = query_json.find(")\"");
        let c = &query_json[a.unwrap()+1..b.unwrap()+1];
        let arg2 = idl_arg_to_vec_factory(pretty_parse::<IDLArgs>("candid arguments", &c).unwrap(), &idl);
        let candid_pretty2 = vec_to_idl_arg_factory(arg2.clone(), &idl).to_string();
        //println!("{}", hex::encode(arg2.clone()));
        assert_eq!(candid_pretty, candid_pretty2);

        let mut query_json2 = query_json.clone();
        query_json2.replace_range(a.unwrap()+1..b.unwrap()+1, &"ARG_PLACEHOLDER");
        let pretty2: QueryResponsePretty = serde_json::from_str(&query_json2).unwrap();

        match pretty2.into() {
            QueryResponse::Replied { signatures, .. } => {
                QueryResponse::Replied { reply: CallReply {arg: arg2}, signatures}
            }
            _ => unreachable!("not reachable")
        }
    } else {
        serde_json::from_str::<QueryResponsePretty>(&query_json).unwrap().into()
    };

    let mut serialized_bytes = Vec::new();
    let mut serializer = serde_cbor::Serializer::new(&mut serialized_bytes);
    serializer.self_describe().unwrap();
    response.serialize(&mut serializer).unwrap();
    println!("{}", hex::encode(serialized_bytes.clone()));
    assert_eq!(res.len(), serialized_bytes.len());
}

fn print_cbor_decode_encode(cbor: String, idl: Option<IdlConfig>) {
    // decode
    let res = hex::decode(cbor).unwrap();
    let env : Envelope = serde_cbor::from_slice(&res).unwrap();
    let pretty: EnvelopePretty = env.clone().into();
    let env_json = serde_json::to_string_pretty(&pretty).unwrap();

    let mut has_candid;
    let mut candid_pretty = String::from("");
    let env_json = match env.content.clone().into_owned().into() {
        EnvelopeContent::Call { arg, .. } | EnvelopeContent::Query {arg, ..} => {
            has_candid = true;
            candid_pretty = vec_to_idl_arg_factory(arg, &idl).to_string();
            env_json.replace("ARG_PLACEHOLDER", &*candid_pretty)
        }
        EnvelopeContent::ReadState { .. } => {
            has_candid = false;
            env_json
        }
    };
    println!("{}", env_json);

    // encode
    let env_enc = if has_candid {
        let a = env_json.find("\"(");
        let b = env_json.find(")\"");
        let c = &env_json[a.unwrap()+1..b.unwrap()+1];
        let arg2 = idl_arg_to_vec_factory(pretty_parse::<IDLArgs>("candid arguments", &c).unwrap(), &idl);
        let candid_pretty2 = vec_to_idl_arg_factory(arg2.clone(), &idl).to_string();
        //println!("{}", hex::encode(arg2.clone()));
        assert_eq!(candid_pretty, candid_pretty2);

        let mut env_json2 = env_json.clone();
        env_json2.replace_range(a.unwrap()+1..b.unwrap()+1, &"ARG_PLACEHOLDER");
        let pretty2: EnvelopePretty = serde_json::from_str(&env_json2).unwrap();

        let mut env2: Envelope = pretty2.into();
        env2.content = Cow::Owned(match env2.content.into_owned().into() {
            EnvelopeContent::Query { ingress_expiry, sender, canister_id, method_name, arg } => {
                EnvelopeContent::Query {
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg:arg2
                }
            }
            EnvelopeContent::Call { nonce, ingress_expiry, sender, canister_id, method_name, arg} => {
                EnvelopeContent::Call {
                    nonce,
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg:arg2
                }
            }
            x => x
        });
        env2
    } else {
        let pretty2: EnvelopePretty = serde_json::from_str(&env_json).unwrap();
        pretty2.into()
    };


    let mut serialized_bytes = Vec::new();
    let mut serializer = serde_cbor::Serializer::new(&mut serialized_bytes);
    serializer.self_describe().unwrap();
    env_enc.serialize(&mut serializer).unwrap();
    println!("{}", hex::encode(serialized_bytes.clone()));
    assert_eq!(res.len(), serialized_bytes.len());
}

#[cfg(test)]
mod tests {
    use serde::{Deserialize, Serialize};
    use crate::{encode, Envelope, IdlConfig, MessageType, print_cbor_decode_encode, print_cbor_decode_encode_query_response, print_cbor_decode_readstate_response};
    //use crate::encode::{decode_canister_request, RequestInfo};


    static QUERY_REQUEST: &str = "D9D9F7A167636F6E74656E74A66361726759016B4449444C056D7B6C02007101716D016E7A6C05EFD6E40271E1EDEB4A71A2F5ED880400C6A4A1980602B0F1B99806030104282F5F6170702F696D6D757461626C652F6173736574732F4C6F676F2E66343530363237352E63737303474554000704486F73740B6E6E732E6963302E6170700661636365707412746578742F6373732C2A2F2A3B713D302E31097365632D63682D756100107365632D63682D75612D6D6F62696C65023F30127365632D63682D75612D706C6174666F726D0222220A757365722D6167656E74744D6F7A696C6C612F352E30202857696E646F7773204E542031302E303B2057696E36343B2078363429204170706C655765624B69742F3533372E333620284B48544D4C2C206C696B65204765636B6F29204368726F6D652F3131362E302E353834352E313431205361666172692F3533372E33360F4163636570742D456E636F64696E6717677A69702C206465666C6174652C206964656E746974790102006B63616E69737465725F69644A000000000000000801016E696E67726573735F6578706972791B17823E88F636F9006B6D6574686F645F6E616D656C687474705F726571756573746C726571756573745F747970656571756572796673656E6465724104";
    static QUERY_RESPONSE_SUCCESS: &str = "D9D9F7BF66737461747573677265706C696564657265706C79A1636172675924B74449444C046C03A2F5ED880401C6A4A19806029AA1B2F90C7A6D7B6D036C02007101710100EE3B1F8B0800000000000003ED3DD98EEB3876EFF90AA5073728755BBEF2568B8D19349005799C870498643018C8166DEB5A965C92ECB2CB70BE3D5C252E8794E4AADBB9C80CD04B59240F0F0FB7B3F3D7D5362A4A54793FFDE77FFC5BF0FCD362B88C56BB617942698582D1E1DB78F532B9BE2571B59D9FA2E221080E511C27D926989EFDC53EC9025BD916259B6D0514F0BFB592F0AF3302302A3649360F3DA87095A779C19B2DF3F812D00FFE8260BC29F263160787BC4CAA24CFE62B9455A858544594F12F4D2D6FF85CDE7EDDA33889BC876608DE281C4F0F67FF0A9240EE7C9563E059C5FBBFDDA006F375BE3A96D7A65731DE22C183BC04559255BE5744188934D890FF63880FABA458A56840D13E4405FEE48DBE0CA096A32FBEC706F975340BC3F00B8C45B4AA921392D008E481E0D65582612A342C9377341F618816EA85E5FD03BE6FAEECDDFD205466B87C475AC7457E109DBE6DCFDFE2F85A932F5A96797AACD022C9F0469E870B831A0282B685F857BA9C8BA8C4B313BCA1E52EA99AA275827B341AB0AF6C201DAABD074916A3B35ECC3FFB8B2A3FAEB601A11E1ECD3ECA92C3318DC88FC5EA589418DF439E90299048112765B44CD11F59C1BF9EF014951A7D78DB24DBA222A9161C468068DD799667E81627A77A268B53854E9B86AAEBE48CE2458AD684A2053DCA3065F3AACAF7108957511107CD574C9BFC1C94DB28CEDFEA138BB4C5DF10C204AE4B0DF2C8D56A128983902380B2D34319AD5180176F14D079E745A4E322C6E3AC308509F60159F478710AE8B4907D0BC667A53A1D654B7DD7B9E9226781C8949E904C3969FFC9E422535313253A56B93EFA109FB8048BE35E74F73C7B7C7CAEAE41501E922CC308D37D35090FE7DBB0DC4769EAAEB88AD2D5031B709A642860575750565116E379F5BD9FBD5181F6FE6D884F85CB87610D67145879DA68A0E4AB5406AA5DA66A11DE2F7BBA5BE6E23BE937C29396575185BC71E9B10F5E92AD932CC107857974E0E96798CFC22F5EE001FD785FBDB1CFF643978A815847D3D9E18C7F9123F7B85F233CD92B4EA4C970341DCD5EC68FB3C923A60A03C55A112A5120986C05EFF06184FB1B8633BFAEABC0F4497584579554F9D95A97123FC90861B439C027CB218D2E7356182CD37CB55B18CBF8C6AE10AD6D5915F90E057184577251602050DF0B5E8B4DF6481CFDEBBCD80739DE8298FD21B425FF86D2AD802FA6FC80AF90CB9CB56FE63DC8A23DAA279F4367931FE0AB45AA181F0BB652A6A5F4B54AF66473AD8F193B8157C765B20A96E83D41C5C370321B8483E1783618494B2DC0AB8881C297C731ABE6F5D2C2677EAA6C6C860F065A909FFF4CEE1FF9AE934896AFD784111D8F677BB1646FBFEED0655DE0F1951EB8BAAFE1976B4DBE39FBF610FAB72A373F4F1EC3186D30BB060035A846001BB889FD4716A5BF80FA1D8D87337B43442E065BB3301CF56CF9A787D1331D92C7213D6130748863071216EC5DC086A113391B3DC64F8CE093A7FE4491DB7E0265468F233E9A59FF897542BB93361CE2EDF18E0523B7FD04DA8C6762344F9FB06C646877D2E6850DEFF98E652335FD04CA4CA6622CF83CF928611A600ADF393DA4972CBF0A7E47621F04F3E7FD8239AC30F4171A6FFA6176FF36E482AC8E8CB803D7293A2FC87F82382910BF1D30C3B0CF16DF8E6595AC2F421416325C94261B7A3BEC4BFEE976D0A16BDC9C22E9130DC0995F8D12294AD6795017322EE386299F48D7F0E8F9BC5C45CF8348FBD06540153A57418C5639BF2229177A179BDF70F6D23D476782F322CAD58B820C2F9A286DF422D2B805A930E35D335B0C05C6975386ABD69A986DC285C2C0434C3D1F42F24EBAE625F80B6F38B7F4E961BE31893DE9A2AFE722CBFFC8303067C556249604E6ECEB1945299E1F1403530B975C39C2E3C39923A7C8F3CD5AE1A22300D952626A17B455108886EA86E35FA5FDD68282F7B31D09EF67AE81E2F26C0D6B8B0583940807D088EC85769589D092046B2C37915ABEB90DF4BAEAC04569A357E0731376C0DADB4E9C88E3F2ABB3B32E7D0C4F517A44EE7E589D4FE82B8D96286DE98BD6F984BE6254AE8AE4400E97961EA59A1DFB4DF0277389015F01A95F1C4A05A0137E3C3BFBF80316D5F08D579455B0DA26690CF7A9D702D463A0427A72F617ECBAA19A0608BF3155511D8854C64FDAA0169783512D618AEA23A2AB563744ADFAD3C6693DE35CC5D72020C5789868B50B981C1AD87628118ACDDA2DF33D24E79BDEABD0CC63A6214D563B7246E955BAEBF11A10B63397B7C8F22AC0C750FE86F91D053CBD9E219DDE50BA931C679E43F7A481B05D376DAC810AC6463432C2873F0B2AFCC59F6FF3132A06F7B5B528C3F5EB8A8C15D9F08311B2AED58E402866DD6949ABABC4A4027B7B4B9937D3B9DF5A29DF5C4BAAD2D1C242108D838441D741A86CA29B3911C0FB2E93BECD2C2B24C90EC70AA4F7DDEBA2B57DCF25D101C54E34A35FEB6EFD3B49A8413199CF62B38C1E603E30C065B8DF6967226B9D75BA243BD2FF0ED09C5B681FE073334000ACD6B3A5176503EE516595ECB8BA960A78BA6C8A8FA715BE2251F58650B6107AD7FA369F879E666F86FBE21CE13C2543CCD7417539A02B151BA9D43BA7BC03BEDBC84D8B4F96BAF5D3EC354463EDA72A92BA3196856A7EA2F33E02A968C00D95FE62131D74A99AF33EC13E7F0FD649253A12DFE54FAA79BEEE897F1E780673A41EB475036E9CEFCA142CD6B87F6642D100D505038F0B3F94ABA1E7015EC2C45AC64553A7F4A54AB4FC38718AB48BFC589155255F0EAA245DE3A8140A1AA9723641BAAECFBAA7A31D532B9565D5B003C0A3D5F515D46D3CB5022527F6A70B20CC1997AD329F7AA9A23E12481B5C1CBCE6217E8D4A3E2DED8459FA5892C1D12383CD095DCFC657E383BED2F0969B8F168A82434C0C4527A0452A1DE8DE171E1F0C69D57E64C1FD6D8B77674037742D0DE02343FA3CF0B2FCAD880EFE82CCF51A33BBF36D12C788EBA4EA8F284D93439994373AD77F26C7CFEF05DE7F69C1A2FD3A5506AB97F6DE4572312A8A9CEFCD8152006C5EB5825CE2FB5DF569E5362F2ADF43518902BC8307923B45B7FAB4D7F6BA4956FB4B4487038A30722B24AD4BFDA3FE5BDC3E0BD9EC0B9C109AF517ACD1AAE6132E54EA4E30ED9AFDD796E029FFAF4FA33B3067F721BE8657689BA718D7AB5B7F774F171F83DE306CEE7E061F69CC2E192172C7681D1DD3EA43E8CC690B14CFA3355E669F809C0AF0DAE5BEBF67B6D45EA8B303715F20EA9E11FE3FDBA88FF82FBE2347F8EE1627A2BCC01D2727376E841E9115C8BFA169C59A523BD142B088CC0741F0863FFD64FA6FD4969CDA096AF2548C2FAF3597298A6FC32DC2C27F510FFC254E56D344FB0928F09CEC2A37FEC8EC52B78E9810806F39546451EA77D6202D3EC81158B0F3F86731309D2C9C715068615ABC0CF2508313351376A40A3B51B9539D58400FA3E178E6DF96C7AA9284A996F9531C7C24FF35E1D06675EC1577536D99691B358C9887C53AD07B982A559BEBBCDE024E1359DD07BE5102D503ACAD3F3C28D11A9D0F51164BCCAAD4D46A6B1F120EEDD0BA7124FD70B8C83177975417FCD72929936592921F82B56BC61EA5A9DB30D84378D338C8066DCBA01DF86B12A418CC481E0CF1F949A2D43424C3D06BEC88AEBD632376449061FC3B1DAEEFB61FDF86559EA7CBA899A8A7D5A1884B87BB6CC00F9C52B864CDBEE06FFBA87670A49A7EA230F002EF615C2BFC9556843D153B645324F182FC07B3FA7BFCA5A272C3719F09CE4C6DC9C1351DFA6007DD4DEEBA5E9C8B1D8C2E815A48848F0C11D9949457C9818FD9AE11A8E1B450DC63DB6D602B1ED61DB225AA4B87785BDC868428FA4C52CA3282CEC75F279DAE2699486555A06AB5ADABE27B612DEA35DE075441646A52C421428D2A7996D6DEA0C7D7F169F7FA371629D1420C3133F4CAFD001C57EC85BBE96F1F1FD082CFF78911F80422DCB736BA74FCC3CDC1F78CD6D0F8BBE7F1EBB82CAFC6A969ECDB16D7FACE606FC6B5BA5E67AFBB03C0D24BE79C6932E4BBCC2687ABBBD0F482AADBC3DE506D2A19CEF978AA2BF27836184E1FF1BFF8FF2F531F93DBD098B5EB4158CC03E4F10479513591178CA503790F7CC4E54711801130E7C08117CA87BE60214E6FB51F97AC417746A3C1F3D9D4A4CCD41D10E49171BDCC01737518B10CD51348E242D888489C5BB92AF234ADE56C13662D17E34525E626388BD9A9BF5CB465D2140C3C321CBFBDAB3F10274AE1D37835C38AE6AA13A51C6D314A8FC9CB667B3D913D8E6786DB6FF618C914C915ABD7ED3E8B35C7ABE1A1C837052A4BAD96213A70B60E119E867DC28B82081392B3A8F0F81F3F126D00FE8FDCFD319B7C5B9DAE4D80078FE8981A111DB24B3B778C777AC0EBB25AF9B29BEEAE563EE40EA6C67A068BCE60FF186E10AC2B4F465171DC683FFFAECDFE3EDA6C20580B3C28FBEA3F546B66078F54794D913D8DAF0FBED0F8F160CAB95DD6CD8FA2A9EE866C5FE57447A89F0D908DF32DA9B649F6A392D603BF5E610648F50CA3E3619A45AE40AC4F391639442E48891BF08C6A4D05C076EEE9425E4B1F9825A2C177BAD7A1EF7B1DDB3BAA51152568E0FF909DAB31EC8B585CFAE58DDD36E2DB77B093BA1703E36AF435E1228D658239D9AC13D36F55F2434AD5A5C3ED55B71A3E00E57096D9B1763D916495041D7547866EC174B230D56C5CD5DC671092169778478861304F094AEE1606C1C4820772431E088D9FCF68F2BE5E4EAF502888518BE2C88945816B7283C65CB2ED77DB4E0598D5EBE37933AEDD6AAD2A9BCFDF170BC967892F042952B9E6735DD2A83E0A9782E6D8787744EBF5AAF124966C0B69529210E94B2A163CBF3B88E14BB4DE45CB6D5CAABFDA0C5570A80FA40C047CA6642E84E1C315B1ED7EA5CD6D620E47F79501C7556F2DA1956875B723D1CED0DE8CC544744042F533E2A8F4B9D339D85644D6B973F43D7CE3000D2D009F075868DD30739E3722E1755F6EC0D8754AFC5DECF81E6287C48699AC02AC02B72B713A48178A57DF47389BF625F3A3481CED88DEE50AE386D8D7F3C57008013721770420779838D2D6094ADDC7A9E9E734647C1DCA5A1A02714BAC53953FC47006DE23B91C25C84199E6EE1305B6D393402361A06F7438F42F624CFCAF8700D7B058877C40AFAC7A8FC62B144793EB87587A6897A916284897E45B54CE3CA343ADFA7F79198E9F62B411DAFE6371C0CB651686627018930D0A841E0ED3CF1F580E64A50109FA0674D56243D69A6A9060105F0ED6E3767ABA1971EDDA5B09B3C096FA7C264A7C0810616D7BDC2F555D1C55A271E643E14AA4EFEA94CC20A57E1D13668946690F6EA6CC33334BABAAFDBE7E3BBD4E5695B26C2EFF3F53C93AFAF26F72D8EAF6288E5946AF0F7C1EAE76573DB230C1476473A69F77974BB4577FB547EEB533F89099A35645422878EA4F6954D4CCB18C8A4F40CB6903EA4018FB6DC43C21E55445CD75446FA301BB92BA99DF94554AD112EBB471BF504A9BB40F82F2B24B0EABCA3DF758B90F5AD91636EF267A4B2A26A70ED4EA6CEA6A9C9D227C733446DC5DFE7A3A54DACF7B726F58655889DA14CD800ABD814C433E47D4581390752308F88BAEC78256D7D3E3736D60EC3238B72CA78DB6C8DF203F433B293DA61B0A74573D88B2BDDC362D94558FE186B2BE0B45762CC31846E5812816A9DA693EFA3A52CE99BEE87372C58DFA65B57ED9BDC56EE99A0D66872E3CE0E5102545A0551A78CABCF90A364B2CD5110EC942332DE8814ADE71E54411182B20F5C7715F20E208E6A70253F186E290E00185FCABE1FF99BDADBE25E81E4415852707037B9EDC37E38D5AAD49BDC3BB6962F81B4B7F4DB5621ABDBEBF39FC24CD93895E7B789B7205A40B6C8B73013B8CD4A3BC76A4389C9BE3483BE655FF8C4E5E0C76243D065CCF01AA3A14EC51766CBC0A5C37AE83C606CEC090FB205AC70128A9A5CEE92E3C3D396614D2FFDF73FF58A355456080AE9786B8087214DCE1135EAB9CBA6626E554817008796EAEFC80B28EF58769BEC9832C2BF5FACAE540170DAD296E08D7C2B18134974D7DA8DC8634C787EC2DCB1BC93E98CDD1A0473CD33C285D173A1F9AF73863F8B6754D55CE3C4B36E359DB97681DA9C00C37645AFE8CB933F47B8256AD215A9E979B59A6868D709FF4459DFF97FD84D5858E212FCC699B3F926820791A241ECA98639D85226E31FA39266F781F08C773AF693B45EEC671D282E38DEBB7B4FE3A9C1A3CEE4975B13EBD81B214E79A17B27D2DD01D2FA542F709218DDD9AF5D41F509C3E0110BCD2DCB219A36895AC76178DB0B234443BAE25A58632613D29F2C1D50B4007ECACEBCB58E444946222897A79E5BB223B22D77DD4E55EECC087D4B6DAF7F3F4E5AD7273BC129B0F5C5D326746C927D21AF4B6A224D90248F2A81C3924D3A36F2442A87F07DB99AF986A59B99C669AD490BD2B2408EC0B5E8B29497D09653B540D2314352AD528D196A67B0C60FE0EC2BF8E6767D5B00182B3C5385057037D1E990A76A07F65D1AB36A3BD965F86AF08DBE8A859DD548ED15912A0AC3A19A996B075EAB8AA494F2C03D22AF1E8DC5E1D79FFF407259B0A3575AA7E0CBCA22D606EE2F0EF9CD8825626C4D116EF798DDAB20F03DBA25D5D15E02817DF605F880267B8CFE3A8917697CB645FE93F75DE5E9C3C1A1F400059DE2780D90B333EB0A5FBBB556C1D6E5A63F912BAE236D6856B96CB6F4208ED506D9934B3CD998649D3311FB6D66A3AE41A13C91A6321ADD728590C7D0AA7369817951E2D80271E65383A50D7D46B30BF289155C4168119A548F25B829787BCD9687D9B6294154A7A51F2A7B2259B1A9CD50395B3AC966A2077A3ECE921CC62652BE738832B0874A9231A95CFFA34B4F6DC71C2853FA674CD1AF8FCEC8DBDAFDEA45397728CAABAB2881CE5EA853E8500E4F5052BDA219D7985B00DDB759E57F6A9096DB3D08950008B387B7AA41CE23D48816C18CAE25B33461210966F7A6C19D6C0B66778A9BC691A6156ADD2E8C11750B92833369DD405FB3EF0985A03DA7BBCB2B6F97E2D8F8703E61A4BEF412828D9FDB32279308F58ECA601BBD77E5492301550C94B0D41748A126AF17069F7EFEDC94D935BDB4C773B693878E3A891055ADB2963E9B6E73163C1A353378EA365D16D9CDA77C2078164D3BBE910AC3E5A17F456F4F01F9083241C69056831608CBCEDD8D8CC9FEFEC2A458B7365FED87480B561A8C54969A4EB1A70AEEBE5D5C8F3FAE0EB83038F5E1DF46AC3A48D5E4D98ECE1486DCA93C976F0BB33F33DC00BD3081264DCB83554D034BE42E6B71676BC8FFB30CCE8691418912C415646BC2DBCD65E4FB2F6CB4AEDE7A7223C255758981E83FC6E580B575D35E76027597492435DF49EA62A7B21231078930355B36A202085523F0CDB4052733FAEA2B7EB916845F6647345134B5EEE80C8BA1D09143679943F1672966D7D09F31AF0141BB71C35B6F6F485B7D92C99BB6D87A7305A8F803E2E047CBC9D93AA1CA20DC9834E9D2F933DF9417B086254EEE8C2A4029E39B88935D106536572582A27C0919375D7AAA204C6659F2F13E21E7FA72DDF413E179692A9BD3E842D553FF3E5177821B5AD013B6E720F524A2D6257A284D6AE97EE23EA64B7079806C7489C28499088D667D136309E07A8C7C060D719D01BE1901F58226FEE987C5E8F5EDEBBA9C9C42D551B09754D0D07A659E21CAF0F72E4184AEC3A31FC9016B2BA99D7249FE87381446B8EA7E381421B8766341E8FD303533190E8085DE5C9D26BEAA224FBCABE18B7859940891F13F25B96C009DB4BB317C0CE809D92DE777B11A8B35AAE68F82A3EE17486CCF734F86415C2472ACD4BA4B5648B56E58BE50B3EDFADB36F2FF882E7EF0906901A1A5019F27677A9F7A843BB22976B2EF25C22D7F342884E3B4C9A9AE055191A3C61669D36A7F6E60D1E0B9E2204849706D427F553BA6E7A7C8B8A2C919E7F72F4ACB31BBC69802FFF6D84053B092A775DBE07EA871E3DBAD9482947967F28CEC2B52E1CF3C8BBB40C9DAFC2DFADD7EBDF62E1F5C093BDB8F49B2D3A4BF79FB1F23A82FED8F21B9615AA5DFC96936F8FEFEFEAAF2ECC33B9DAF83DC53964A51AE6244A2C1DE365593D8C074CE0867C29C96AE65A16587E7016E25ECC12DFEEAE043F6B64928C721A27D44C9DE3392466FAB5A57CEFF7965FE394369453C4437334204ECB95B38A712BF25CB249F6577D6FB1061ED0EB609861B6B3BDDE953B1458F2F358FBD4A0A84F4CF15DC049D9F101ABF646F64EE494200E3201A477622E612905646B5DB3D74F26EE9EE938F07EDE19D4AF4B1CC3C3B0CDD9748CA3F1296EA1BDECDDD146F116A0668A6E39CD53EB9A871E76B1F0660E8F19FAF87B9BD30CAFA490DB3C392C73AF57ACA7BFBB78008B04A169A7EE9CE71F24B02731D29CC880AABEEDBC123A0F5BBA9305707EB8A9A9877F4A13C416FBC0F8E263963B60B7529DFA71383B9C5924BF6D736903F99C5563DBB0F2623FA82E7410323CDBC954CA7642FEBE2F113B10353354FC6E5D288C2514C6073DA27EA647A2745707710164A40716AAE4038367B2C3F412BF81CB35DFE4D66AF73AD9606A6969DA1FE37817BB7368CA427E972C04BA9F08EF729F904C0F7ACF0DBE25DE78E84F0FC1C886A5F7BBD782596562BC4536E47D4FE52C562E2EC6528A07C9FE31D9131137CAAACEB0491A4699AFA55C940CC798CCF431DF5C4EE218994D4FDB5E6A61B5B9A435326DF7B57EA736DBCBB1233E184956EDF72F97675980BB2FA10C4977B4E27C61253D2AC7E17F28859E1B36731FCCF27F691E78F5418B75A32DB1E85D05B2D6B049E2EE6734567AEE0C8A3AF09568B397137D2EF375F8060831648F35024C926119E6211C8CD6E4EDB3BA9C6E2EBB86F7739DE4BAE662B019577A9F178AB981D32DA8F78FC800230AF8479D17F1FDBBC0A8108076EAD3EDAC903B11F204F73FAB904B30F801F3CF71B28AAAE638E42B020C121A8E1F4B2D61F074100E86E3C10848B84AA306EA346866F40964D27B231E33AB2DC93FD0F43BE00F2380C154C03533891F145537EB9AA40B366296292BE70F027C630E423155B4B11AFA5C1311483EF71520B4D5486C48D9F7E88A6D69008C0D275606E4575D9F0581EA9730DEBEBFAF2E162328733A8788FEDF24540B84241DF4DDDFB5ECA70CA83BD65CD139065A76E17287AB919D5249021EBA549BDD4CFDA58657019C8848A1047AC132E9823C7EA17418C0520E70E689AC63E1E3CC7740E134411C9CFD7D24016F349979D062B1C087B874B5AE54CE92A47DF43541F0D569C7B4799679E3922D8B8E1B0A3A39A71CC8D06B97A3AADCA3A696410B05BDE72F2D55EA49F4BD516B65DF9B4CBE9869F39FC390CA6744AA69F6497CE4E11AA352FA4A5D13F7798CE6781BBF45452C1726E478A17FAD3074B2CC590E6CA94A16EDD15CC5AFDC26FB3D914B6C715B9C7C72266D10C235FC22EBD2EB1909A67480E1ADCAC172516CB8416DD0217BDDFE3D3FFF8F9C9FDF1673C6D8CB3EB9F7F96C7F207B3F870006BE69D95159D5C150CA72665D6DFDA316BB76D537401F1CC4D5F0D5164D59CE8FDA3064DB29AC5ADB7E61B5B822A2E7E0C10B1902CF78603C4E9358E62D6E66BA0107EEC6D6ECC8F45C6AEB883F7305BE0211806B01A8E746E4B35F8C80D052DE8DE88663DFA7263A62227BBA72259073C5F506CA8E96611535B6CFD7D338B9D49904E6806726630C69B8896172C39002FE3CA8A2EB540C658A1214375052921A107CDFA617ECD1D6759F48C9083BF87592C7CC369B46A7327D1A5D7667CFF59428ABA2FE525FBE0261766A6E530F496A9411C9890D22ADA4BC05BBB178A6689E9D56DFA119C9CDAD25F883985B408BAD3D7FC803A489C5D41B4E4AEB8323A6D64F165F7F21EFA9FAF752437D5A5652770B1DDC4F3FF51BAC9A7B7C2CBD237BC7E88D7C53BD7C73EF5ED6E245DCFF01C8C70906A9D2AD1EDF539A4362586D11E622559C46A7F1F9981C5AD7BE233C7EDA1A1E3FF5ADFED8CC9408B948DE8727EC6EC15D2926DC95428E4E215D8FCDDDCCFB73BEA46A5B190E583C3C3C5F0764FE8562B6B0EDAFE65A2030F16150834CB64FF14BA1FDEC93C2BB93E7308BB1FD11229BEFD189955591939722E8B781F78CAF3AF22F0912F1428F6A46422C91D07FC82BBFCEB40816569D4F4B92919707A4875EE1F931064A5B07A2B595A0D66AB2C705B440B4871E381AF2760DDD0D8FAB95EC44C601B8CDCC4E88729A78101CCF88EA04429CC35C4074E7311E2FE58489140B200034439BA8DB40F7E546074405473C676FF391948D2F27174681A2DD9CFE37201F9C402937AAC396823FE919F23C843C048CDC98F66EAAE298AD244F065E38E8D0C41BAAD6318EE2F77867C68ECC2A8DF607DBD928149144B921FD4D4C143415237F41A42E630E1004E27C62DA8F5B90B090E313701977C5C58900639E3E7AB977C87D63C18E2BE915A774816313B9A99C570B4D75AF994FC7D3FD6432D343295A5D10FC8599BEC0A6C132D2775165954D9E17959ECF365BD35DF92FF440607145AC7320324439DB3885A0E6ECBD8B4E791661B237211B8A3192D9A59A1C00F0739C648D1C34805A908A968812C8A3564FE49144CFB4CECA63DB3BA0264AD0234D841DA46B8CF5DAAC32FEBA5337AA7CF546F8E8866923B9917D7B46D1EB480E30FB87FF059EF2E8C351AC0000070F582D4672616D652D4F7074696F6E730444454E5916582D436F6E74656E742D547970652D4F7074696F6E73076E6F736E696666195374726963742D5472616E73706F72742D5365637572697479246D61782D6167653D3331353336303030203B20696E636C756465537562446F6D61696E730F52656665727265722D506F6C6963790B73616D652D6F726967696E0E49432D4365727469666963617465D30B63657274696669636174653D3A32646E336F6D5230636D566C6777474441594D4267774A495932467561584E305A584B4441594D42676752594950756D693959656B7A4F732F3244422B4F532B374F534547306C556772504473324A306964776D30792B6E677747434246676777365A505961756C56576146752F746D75766875574161583447715332335A6135547A426937537973766D44415949455743414F352F796D484C6A4D536E6156372F5159693278466351732B4E3959354C45754876784A555265674A64494D435367414141414141414141494151474441594D4267774A4F5932567964476C6D6157566B583252686447474341316767676A4B36524955375058612B5A6A35456B35705954525A4D307A47482F556D71694145794C4B684157496D434246676732505A506576796D7056314F3574375A7343414C72475A52797654486F5A494345725767504A7678337A6143424667674363393067316E416C687A7974496A5179306D4E463665564E4968366959636D34526A754E6D5A35612F2B4342466767544E302B50647939306D655557527641777A564A683766345150512F2B716334754E453137724E7734372B434246676732795A6E6C702F56723578314A63462B446E7A33453857416850436C794E526F597A74736E33684D674453434246676770647563364F30304D4777383647555058437170626377433141775A706F714F55595858444B4376524D7944415949455743436741416E4F7A444F2F6943456A616C613965627346694D763230546B4C4D7269447472487364496B746F594D43524852706257574341306E4270657146396569747842647063326C6E626D463064584A6C5744434F482F6A4E375445385553345A6555394F4B67746D69757A62486F716377765134614C4C436565447077485A46526F374C3434332F624B6C457343417A4945773D3A2C20747265653D3A32646E3367774A4C614852306346396863334E6C64484F4441594D42677747434246676775626C474C334E31524264632B762B666874516136526157596571633334667A69753141596E454851584744415949455743444C6E397A4E6441416377504A5834554248444544587431626D4469697A677241304F394E34533855516D344D4267675259494E736E68486D786B55416C665376442F6B484E4539556552314A56346979676D33567138554959496254706777474342466767532F395537704947496748726C706545416E626343546253527150334154477A634D754862686534784E6944415949455743417269704E635476393059646431536C326766664C5050474353352B3166754E4B2F694C65483246636B53344D426767525949416C38457561525950426D496D2F366E63745067363475537379587375706942765A75757754614A434C346777474441594945574342705049416B573239334D57496972526169622B55724F532B492F704962646D447937454D653345545537494D42676752594943765A78445A4F445A6C61304E644A6436627863726552434A48454F654753317762435762634142516362677747434246676778646772495770514468724A79532B5771585554556E75453356785A4630344F684C314E53334F6E315769444159494557434447517133736934446E665274424C5A5347764C684E79566377643039765256666377464E776436354268494D4357436776583246776343397062573131644746696247557659584E7A5A58527A4C325268634841755A6A6378597A5A6D4D32517559334E7A67674E59495077566E794866456A484F4F42724F6E51447959614F6D3977316D48694D5573394362386779526B2F2F6E67675259494D71303766797A567A4F67556E756D445848563543546B583652336F45665A69316C2B4B7A55595956367467675259494B45434F4675656A6A667471424558636847586C4D764A6C7836726B6C363276687466793656307635714F67675259494366306743596A59303175314438457253544F4E6D42715A684678486D67704D617A4B305550304C4441663A0C436F6E74656E742D5479706508746578742F63737310436F6E74656E742D456E636F64696E6704677A6970C8006A7369676E61747572657381A36974696D657374616D701B1788B74750BA92C1697369676E61747572655840CB5A58F906EA73610F139C16FFB2623108B40248867B257E9FD2DD6F75230EA3078392B454F082BBD1FD04C84F9E2D7F795FD65BCDED47E9D8D892ED09CED307686964656E74697479581DFD8E4151C4762D9B4B6B36E08D36A49B83E9458A5040A1DC26D63B5302ff";
    static QUERY_RESPONSE_REJECTED: &str = "D9D9F7BF667374617475736872656A65637465646A6572726F725F636F6465664943303330326B72656A6563745F636F6465036E72656A6563745F6D657373616765784F4943303330323A2043616E697374657220716F6374712D67696161612D61616161612D61616165612D63616920686173206E6F207175657279206D6574686F642027687474705F72657175657474276A7369676E61747572657381A36974696D657374616D701B17891178A3666CE3697369676E617475726558402B9A27D1A03BB4D823CBF41EF42D4A381D22AF4B1DA1C9FEA5B54D2DA9A1936621487AD41F4F8E568A2CEF1F314FEAE5F016DC9F1F963D346B125FA620F97D02686964656E74697479581D9F00EA7B0826AA145ABD4348C3656133ED84E188780F63C5151009BF02FF";

    static CALL_REQUEST: &str = "D9D9F7A467636F6E74656E74A76361726758394449444C036D7B6E006C02B3B0DAC30368AD86CA8305010102011DE610F825BCD7BE6F76432ED18E2811EBED0863B4B220CD2059FD12DC02006B63616E69737465725F69644A000000000200001901016E696E67726573735F6578706972791B1788B7D750B6CA406B6D6574686F645F6E616D657069637263315F62616C616E63655F6F66656E6F6E63655046F6152828E8C20B7C08C9FD2C02F0676C726571756573745F747970656463616C6C6673656E646572581DE610F825BCD7BE6F76432ED18E2811EBED0863B4B220CD2059FD12DC027173656E6465725F64656C65676174696F6E81A26A64656C65676174696F6EA2667075626B6579585B3059301306072A8648CE3D020106082A8648CE3D03010703420004A0B28B61D6940150E69FF597D9C8BD1411AADDD8D3119E1058F0C3023E3B2F6DA8AF79F0322629EBFA9EF42E9F9487BC27414F91E41F7A7F4793986EE834227A6A65787069726174696F6E1B1788B941235FDBDB697369676E6174757265590564D9D9F7A26B6365727469666963617465590463D9D9F7A3647472656583018301830183024863616E69737465728301830183024A000000000000000701018301830183024E6365727469666965645F64617461820358204A0A49EFEE8F05F6D31002ED0A6043E2B2F3D945D1683F2B5F884C25011B559482045820D8F64F7AFCA6A55D4EE6DED9B0200BAC6651CAF4C7A1920212B5A03C9BF1DF3682045820C751A1F91750E652047B3E46AB16F421193CFEA4AC9C48E83617CFB8B062E3FA820458207B48071C1EAE0E18530C79D53F35C71AE1063596B35B9124916DDC14636584BB820458203072D868CF089BC25967673A29BEF783994A9109EB04962A3104D2EC1900876782045820D7341A71A0A1BBEED057D2C1FF701A1B2D1BC986C6EE414585FF8F750529A72B82045820E4498CB3A6DF19D5D3A8F86C7A5F9B3E377E4005BD08A10ED19F79333AFAF6088301820458208C9C651D68C1E098B7C933DF669AB79EBE5F490A3DE66BC886DE95657154051883024474696D65820349DBCAFEE7E5F3ADC417697369676E61747572655830B174326E54C18F4878CD4394AB7BC9F2B502B77D1C93BE1846F58F37D9BCE99F5BEC4DE1C224E32C76F55C934DEBE1566A64656C65676174696F6EA2697375626E65745F6964581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D026B636572746966696361746559026ED9D9F7A26474726565830182045820DD78DEBA359050404AEE97D4F8D8CE9DBF2824704EC0613AE4F12890E7DE1EF983018302467375626E657483018301830182045820267FE55111B56E3C3975532EA3373F7B72E9F82072FE8E607ED34486478A5B39830182045820466A70286CF9ACE9801CA53E22AF6EE059A094FD60498606D484B6854058307D83018301820458208B2F6C15078AE4D3B93470915CA53E373327F37EA74BA1B8177D986BB79B31AE8302581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D02830183024F63616E69737465725F72616E67657382035832D9D9F782824A000000000000000701014A00000000000000070101824A000000000210000001014A00000000021FFFFF010183024A7075626C69635F6B657982035885308182301D060D2B0601040182DC7C0503010201060C2B0601040182DC7C050302010361008675B634A43E39726238CFE39C9518BC3E3225CB6F5A8479BFCF2B608FBA6F8524DCB80F35A8AE44B47F262F0A6620D41279F06FE0C53A739FCCA01A48926FE651A3519B5B329FFBECC9F0CB908B098DD3E8845CFB99C56379E049AC465EC806820458202C51DB7B5650B7A3DBBB8530A7449CC6F90144778B62F20F3C26D72E95E5069882045820A7F251951EED726811460449388214773C94153C758AFE3AAA54F9B5170426868204582053500B9323CAA4E30ECFB70FBA000E908670EF4DBB1D8CAE818051BE76656CCB83024474696D65820349E6D6E59C87D989C417697369676E617475726558308DD37AEA0FF4F474F75D96278E02523F73F84F59CC9A969756FDDD25B7E837060968B08D7668AC5FECDF56B9095D1B9D6474726565830182045820BEF3927A4A7342A683C7FAC44A60D1ADCFD36EF1ACEBF1F676E412BFEE8554A5830243736967830182045820A02C85144577092045EDD9FA02BE9D57EB442B730636A66C56C0B5455809C481830182045820DFAD228E456D54B7A943D52EAAA8B4B8F41EAA9CA4AECDA31B1BCD7C16B89AED830183025820B879C8FDCBD2F5ECECA4D437B120B460FAFD78AEED1816D2B12664F023C770D083025820D7FAD75F4023D71FC3171D8A62B3C413C405271147F045A2826E6180042BA95282034082045820FA5909279DBA35CAB154E6B0FD366DEE3CA8E4BC3BB8AF181B90295E007C5FB76D73656E6465725F7075626B6579583E303C300C060A2B0601040183B8430102032C000A00000000000000070101256BE5ADBF2DC79DB139418571658EE831D2017646E834C3C63649BB2DF966B96A73656E6465725F73696758401531F901FB44B07A60BFD3C6A1BA4B6DB23640AC5DADC0CAA933B21AA1545C90F996D13FEE4513ECCC23D30CD030084B6911E9DE8989ED3590FACB7F4E36A24E";
    static READ_STATE_REQUEST: &str = "D9D9F7A467636F6E74656E74A46E696E67726573735F6578706972791B1788B7D5B71C644065706174687381824E726571756573745F7374617475735820A17A5542E8C7F32BAA8502DCAE27DB305FB7FE54FF7BBFF2FC5AA795D81BDB126C726571756573745F747970656A726561645F73746174656673656E646572581D5011E4DBC945A6220D42668FD5D967DB6483C47FC6CBAC18ADF42E5C027173656E6465725F64656C65676174696F6E81A26A64656C65676174696F6EA3667075626B6579585B3059301306072A8648CE3D020106082A8648CE3D03010703420004D01E5F3A9CDAFF87DA1BC8A0F03F99EAB15936E394874AC5D04AAEB6DCEAE1EE569AFCF61190D0B7032040C33AE8BB729AB7C1AF12AFF8FB65AE719538D0C8AF6A65787069726174696F6E1B1788B826F56078806774617267657473814A00000000000000070101697369676E6174757265590152D9D9F7A3697369676E617475726558483046022100A98A1488D6BD769523386057745700A916257CA7A301A1298CCF3577DAE06485022100DD9200B8E0472EA586FA8AA340F2447989D4597400CF56E33FE0EB3097AF890870636C69656E745F646174615F6A736F6E78AD7B2274797065223A22776562617574686E2E676574222C226368616C6C656E6765223A22476D6C6A4C584A6C6358566C63335174595856306143316B5A57786C5A324630615739757A5A326571695138507167616F48676874554B506B5F385545466E354F7A5A744C5655535F54736E795530222C226F726967696E223A2268747470733A2F2F6964656E746974792E6963302E617070222C2263726F73734F726967696E223A66616C73657D7261757468656E74696361746F725F6461746158251609770089A8E88E3FF1DC9D728CB1C7CEFF949C6B4F8289B0B2F5FD872CAD3301000000E36D73656E6465725F7075626B65795860305E300C060A2B0601040183B8430101034E00A5010203262001215820F11387E588F717B3175744C1288E23FEE75A69DA8A015476918897683442463B2258201DAA98EDC8815E81D18D4DE72D93C6BC1BD507BA83A5C9A3AFB6B5AF975C41DA6A73656E6465725F73696758401D3E1F6E6BD4FC910BE9CB8DE1C86D3347889C279D43E9FF4D9A78A00515A9A77A6BFB481DC307355C1232D6185CB27CE6B868726AA961CC7F5DA974D00AAD17";
    static READ_STATE_RESPONSE_PENDING: &str = "D9D9F7A16B6365727469666963617465590505D9D9F7A3647472656583018301820458202475C9A52F1849FA028944ABA702DCCA9411D30B5F4FD28AD6D430489676C1CB830183024E726571756573745F7374617475738301830182045820DB35EC13AE19DF080190738F2C63F7FF413A6DA365DE9C7C03475ACA68BD70B3830182045820794CA7FD0156B50ADD2716FCE1712E0966E663F94435EA5499D96B035ADF5FAB830183018301820458209F4BBCD5242484598A06B931DD2063F954392A10456BB2290B259CC4BEBEDCE8830258209F8FBE87EE5AA5E236EFB6D15DE7EBE79BF4F4D043BE4556D5EE81E6864FC088820458204EF0D1F15179977A02D9992B2F1AB59BF9E54837C72EFC21BD2F92C8EB75FA63830183025820A6A5AA660A073D9C62C47335FF9E41396FDDFB54BF1C2A353C1E49E74928B8E882045820CBD8C9898E5A4A1857A9B28E5783B011F0BD09B011947BB6FBEE528954BE59AE8204582067E895B5F37922722B51669F6EA9F478B6C28AD0EBDF77BB7855D6FEEFA201498204582070F035A3C4BFC0F3D6E0209FC062D0D04F6F2CD8EE9DC0623B773BE9CB57579C82045820710982CC85AA4428FDE1D2326D3FC5AE6ED4304602A85805E0A5562EDB5E53DA820458206B3A258F03A57A323FF85131FA206918F43A1750F800F17F0EE8DEF942D2FCB0830182045820D6B887DE300C6D8502BFCD740F062816DF50D4D5F7C297E45A9DD7966475574483024474696D65820349E5DFA9F8D6F3ADC417697369676E617475726558309950E9E125847A6A5ECF7604D17452992147B2471BBFFC9E5EBF4AB320CD4ACC7EC503346BCCE566AB497A9231C522656A64656C65676174696F6EA2697375626E65745F6964581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D026B636572746966696361746559026ED9D9F7A2647472656583018204582029F0A6A94F5497692355F1CDE71461CC90C85A4959C84452249CC734630E701F83018302467375626E657483018301830182045820267FE55111B56E3C3975532EA3373F7B72E9F82072FE8E607ED34486478A5B39830182045820466A70286CF9ACE9801CA53E22AF6EE059A094FD60498606D484B6854058307D83018301820458208B2F6C15078AE4D3B93470915CA53E373327F37EA74BA1B8177D986BB79B31AE8302581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D02830183024F63616E69737465725F72616E67657382035832D9D9F782824A000000000000000701014A00000000000000070101824A000000000210000001014A00000000021FFFFF010183024A7075626C69635F6B657982035885308182301D060D2B0601040182DC7C0503010201060C2B0601040182DC7C050302010361008675B634A43E39726238CFE39C9518BC3E3225CB6F5A8479BFCF2B608FBA6F8524DCB80F35A8AE44B47F262F0A6620D41279F06FE0C53A739FCCA01A48926FE651A3519B5B329FFBECC9F0CB908B098DD3E8845CFB99C56379E049AC465EC806820458202C51DB7B5650B7A3DBBB8530A7449CC6F90144778B62F20F3C26D72E95E5069882045820A7F251951EED726811460449388214773C94153C758AFE3AAA54F9B5170426868204582053500B9323CAA4E30ECFB70FBA000E908670EF4DBB1D8CAE818051BE76656CCB83024474696D65820349FFEFB2BDE1D889C417697369676E61747572655830A6D7225487C81739BCE260157273430486D5BD8BAD1D80E85BEC6A5F9056CAF76B9DE5290AA8BD8701F3C943D020D247";
    static READ_STATE_RESPONSE_REPLIED: &str = "D9D9F7A16B6365727469666963617465590FE5D9D9F7A364747265658301830182045820A8DD5DD63F1DCEEBD735974D50A89F80C8A1C1295116ECF6959BE198331259CD830183024E726571756573745F737461747573830183018204582056BFA80F5DDD08BB7CF61FEC81662FAE420EA9D1339988897FCEE1261E6EF5EE830183018204582094712AD0562CA278A21938CAC48541EDF7EEFBC431DB2CB3C4DCC21A897FD8B183018204582036B1E20877DDBD80832B9C617FBD8072CE76686C1AECC806EDFD149D7FED885483025820980DBEB1024A8A3699CD382C6E9C561086CC8748E68FBD2F6964463575E9CEE583018302457265706C798203590B524449444C016C02A593EAD9087183E1D8A90A710100B0166956424F5277304B47676F414141414E5355684555674141414E7741414142344341414141414338764D4F6C414141494B6B6C45515652346E4F31634331425556526A2B4C784B67694B4B45676F67766A4551646854413155796448782F47744F52716A59715970495570574F6D6261464762715A4C3746463271473647692B55367A4D4E427854556E41454652384653634C69412B55564167727339702B37757A777565792B373978365775387A355A2F6266633834396A2F76786E2F502F352F2F50575467644E467A69474467476A6F466A34426734426734614C6A4677444277447838417863417763413866414D58414D484332794135756D434D6D6E396D434C7846576B2B6779486867534F712F4C796E6270495962504261626C70764A384444434770767464762F67674E43357A2F6B567446643175546C4A76665339425177495871762F6F444E504C644F7756547030625630734A325441454842344B715A4950334952735A4B396E455668514B427A3564676978755A4275535178585A714B78366B526D6973346B31787947324738396B744C4D42796647577A666575734C683230616C2F7A526D4D746F754D706A59774C52316848504B727659546C745A734474595062793355765376575332566A6C344C69706956337366434C6E67437A527152736372726641512F4B6271786F635976506B39596C4D30616B59484C46756977636F36554864307A4C53375963466B714C72446C4B6955363264513078626953505158725447352F69354B646D484F7348783074724B2B7A6A74524A4574723730624E57362F394E674D446C7979502F2B4B637A636A4330775551707369745164546F2B5151323972475274385532726357504B3541526B41485333576B506E4338324B6137567552644877716756534B727453753154557644506A6E585666684150792B48366246567672575562364132795848536A374F5767336C4334306C64646737743974597771516F50454A72356330315634496A59744A74506B6D534C504F4644336F3758464A7655466B776D7545565142345459316B57475364634A744B52446D5776755851356F61794B4546754830456455755A594C722B6E55385854314C5A7553434C3445754B58684674787836306E4E36446A44435935632B457A75616344466A6B436773466A6347436B7842556A75674F446E746655384262564B674C623262513631327958774B54514871704D5155624344674F4572774F674A3955674A75326C68732F306F776478676F554165675434714D6542524165557A4D526B57684143504A6C31774F59586D5A794D3753424E664B48635658457434614B464148755130662F457A3452572B585873455861494B44625142336C2F41706A2B704C447A5837492F4F374965753242636968392F7134746A456B43362F756936414B626A7A3669757635564E54697170726C744F2B61414D2F593361752B305766547533467A7061414E2B734B6A73367A784C30646679636564744A47456A785736504A763267765950504D69464D574F754A52717433766D4D3657554C41496770646E4878374E4C32386C674A4B654B6668447348532B554E6E7942745A6856766F7470716F484E7945354971487849482F4B3639734563363664564C592B2F706C6F534A70715534532B6445696B447A307343514D39564C61392B694842396E7A72737064586C2B3151414D4A39684F4C4C4B50612B733947534A676A637339784E623336624B43307676463373305172426932704F382B4752716E67574D57595475694335675561685932785A373455507845626F535372476E3534484449782F3231306352416F434939316E493666743042654870726F46686A4833384D5834483361457347664E6168535647536D644E4E73624F4B5153706448495250794E4D46722B767244764E39722F79467055364C5049775633414B45545842586B38516E79494869555168745A4D6C3464364449624639636351786C395A6B306D4C70734A7A5276743531666566416E59594E4361746173304B6239652F62304D615A7A536D436D75554E6C383977434A6145596E5050334179417A444C6A6F626E70736B454459514F65614E6265665066654566446572616D756A776147566D534D39506D675A4E4372524C7A737446412F75506461595453584D313053396B4A434D6A75583466624A7134513659614F59774C5850425567644C365A72545A6765524C6D597471796A68497A732B4A697437383070755335556F363458624D4D65386766726C57684C336F674C75644D546B72444A6354504D726C55492B5963314D562F2B51734B4E526C515652455043474F654D6B7A346A5857573652465945727A5168652F70746A78784D416D767A4B51734B635454635936453871624B2B344335523732457A42336467745A374F68434E7A436F4364615748396F464862796355556866324645544C754845355A31334A6A6455774B547A426F70574E5A475370354353593262575A4B5A50414763752F706A64476165466D43433856485065386A533353727246684B306867314B79616545763257733344326C5255374E7A7355324B4E5943743366703378654B4545565541446B62584C45525657476D345248763342574974484D4B575945384C7157625070734330364575536436306A4E4374696B3875434A7739325938734C6D66306F6A58473848635077744C45476F6278663877742B73784634475A4C6A364D77526974337A5248645662444F6B4E6B426C59754F714179344C64624D617A7A684D662F786D5367594B7537475858734350635A754145556B332B5868594B72665A2F726B724A3234556B627979516665614B63486E617573566E584A6F6243493736632F3038687255337A43314A5935316C4459744642785546532B747454705968622F70452B75786F39684D2B6B35474E6B4630555A763867635A57333548466C5063596152552F345536785146664A615A4170357568547A5433517631755748547A38464D6D376E6E7A7475415763637832514B6964564E385559746E4B397061476A56517365717A54444B716278507572784D4F717A3070347670416664415045393450736C30333071446D77414B6A46364B6B63506F37434138426F77306B454273526769326A59337A47452F4253676244737145306454324852744B574B6A41613538507839592F304366632B2B4F4A5250586931554F347733346A744B4459504B7565574D4148545673464D4156326C386632525749526562706155746B786548476839566E4A63432F2B776C66366C42304F62704754384E6535556F6F517150687A7A585663554457327A7A39323771645034677938527256305A586F2B3152683762363972354336304B7533384D6E396C6164706E396253754B716834794D496579596154743366575A75676659523670666A78503166546131514F6E3471737849517A63483862395373786C41352F4556337A6976734832664647747A776F4D553051315876526E6D6A596C686F6E73414A52757171425379582F3761384D476663785553752B3351554F453834664B425A57644F44317A767457775562786570546743506C536F434E434B52574759782B3266344837366B356746614A3379556167352F6F684E6D31706A566F6547412B7973784B324F72314256455438635545635053454E527053446C6167754C375A70545A533948672F576F7A71386B6C6A44674675646250783334744A5564354B726638457879636B694651694F536335697576524C556C4947666A73564C776E32672F716A7572427A2F423142516E32346C66574A7257367633746633765837326E3277594F41614F67575067474467476A6F466A34426734426F364267345A4C444277447838425A6C2F34487A3664634C4F747247723841414141415355564F524B35435949493D0A62716D6D75646D6D72668302467374617475738203477265706C696564820458204DD76733805FAEEC095964ABC58B1758E832D46E208DE25C6743920067FC49D082045820512516B12E3BEEB478460BEBF754EB7C4D3FE0602B485066BF3D45B08A188CD1820458201AB45568F28EABA8CEBD774712DB13E8BD7A4C7F0533F32FA07A147C814D35C2830182045820188398C60BA08F6B39F5F6AAF4E5E1A8D253FCFF05D346D43EFFC16EF57E6F7783024474696D65820349B9FB8489C1AED3C417697369676E61747572655830B766F2E146592283EB819CF9522EDA9FB3F9C42D21771A9CE3B811486735C8700915FDA3B302E92F2CC68ABF883F53976A64656C65676174696F6EA2697375626E65745F6964581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D026B636572746966696361746559026ED9D9F7A2647472656583018204582029F0A6A94F5497692355F1CDE71461CC90C85A4959C84452249CC734630E701F83018302467375626E657483018301830182045820267FE55111B56E3C3975532EA3373F7B72E9F82072FE8E607ED34486478A5B39830182045820466A70286CF9ACE9801CA53E22AF6EE059A094FD60498606D484B6854058307D83018301820458208B2F6C15078AE4D3B93470915CA53E373327F37EA74BA1B8177D986BB79B31AE8302581D43DCAF1180DB82FDA708CE3AC7A03A6060ABDE13E9546C60E8CCE65D02830183024F63616E69737465725F72616E67657382035832D9D9F782824A000000000000000701014A00000000000000070101824A000000000210000001014A00000000021FFFFF010183024A7075626C69635F6B657982035885308182301D060D2B0601040182DC7C0503010201060C2B0601040182DC7C050302010361008675B634A43E39726238CFE39C9518BC3E3225CB6F5A8479BFCF2B608FBA6F8524DCB80F35A8AE44B47F262F0A6620D41279F06FE0C53A739FCCA01A48926FE651A3519B5B329FFBECC9F0CB908B098DD3E8845CFB99C56379E049AC465EC806820458202C51DB7B5650B7A3DBBB8530A7449CC6F90144778B62F20F3C26D72E95E5069882045820A7F251951EED726811460449388214773C94153C758AFE3AAA54F9B5170426868204582053500B9323CAA4E30ECFB70FBA000E908670EF4DBB1D8CAE818051BE76656CCB83024474696D65820349FFEFB2BDE1D889C417697369676E61747572655830A6D7225487C81739BCE260157273430486D5BD8BAD1D80E85BEC6A5F9056CAF76B9DE5290AA8BD8701F3C943D020D247";

    static ICRC1_IDL: &str = "
// Number of nanoseconds since the UNIX epoch in UTC timezone.
type Timestamp = nat64;

// Number of nanoseconds between two [Timestamp]s.
type Duration = nat64;

type Subaccount = blob;

type Account = record {
    owner : principal;
    subaccount : opt Subaccount;
};

type TransferArgs = record {
    from_subaccount : opt Subaccount;
    to : Account;
    amount : nat;
    fee : opt nat;
    memo : opt blob;
    created_at_time : opt Timestamp;
};

type TransferError = variant {
    BadFee : record { expected_fee : nat };
    BadBurn : record { min_burn_amount : nat };
    InsufficientFunds : record { balance : nat };
    TooOld;
    CreatedInFuture: record { ledger_time : Timestamp };
    Duplicate : record { duplicate_of : nat };
    TemporarilyUnavailable;
    GenericError : record { error_code : nat; message : text };
};

type Value = variant {
    Nat : nat;
    Int : int;
    Text : text;
    Blob : blob;
};

service : {
    icrc1_metadata : () -> (vec record { text; Value; }) query;
    icrc1_name : () -> (text) query;
    icrc1_symbol : () -> (text) query;
    icrc1_decimals : () -> (nat8) query;
    icrc1_fee : () -> (nat) query;
    icrc1_total_supply : () -> (nat) query;
    icrc1_minting_account : () -> (opt Account) query;
    icrc1_balance_of : (Account) -> (nat) query;
    icrc1_transfer : (TransferArgs) -> (variant { Ok : nat; Err : TransferError });
    icrc1_supported_standards : () -> (vec record { name : text; url : text }) query;
}
    ";

    static SW_IDL: &str = "// adapted from https://internetcomputer.org/docs/current/references/ic-interface-spec/#http-gateway-interface

type HeaderField = record { text; text; };

type HttpRequest = record {
  method: text;
  url: text;
  headers: vec HeaderField;
  body: blob;
  certificate_version: opt nat16;
};

type HttpUpdateRequest = record {
  method: text;
  url: text;
  headers: vec HeaderField;
  body: blob;
};

type HttpResponse = record {
  status_code: nat16;
  headers: vec HeaderField;
  body: blob;
  upgrade : opt bool;
  streaming_strategy: opt StreamingStrategy;
};

type Token = variant {
  \"type\": reserved;
};

type StreamingCallbackHttpResponse = record {
  body: blob;
  token: opt Token;
};

type StreamingStrategy = variant {
  Callback: record {
    callback: func (Token) -> (opt StreamingCallbackHttpResponse) query;
    token: Token;
  };
};

service : {
  http_request: (request: HttpRequest) -> (HttpResponse) query;
  http_request_update: (request: HttpUpdateRequest) -> (HttpResponse);
}";

    static II_IDL: &str = "
type UserNumber = nat64;
type PublicKey = blob;
type CredentialId = blob;
type DeviceKey = PublicKey;
type UserKey = PublicKey;
type SessionKey = PublicKey;
type FrontendHostname = text;
type Timestamp = nat64;

type HeaderField = record {
    text;
    text;
};

type HttpRequest = record {
    method: text;
    url: text;
    headers: vec HeaderField;
    body: blob;
    certificate_version: opt nat16;
};

type HttpResponse = record {
    status_code: nat16;
    headers: vec HeaderField;
    body: blob;
    upgrade : opt bool;
    streaming_strategy: opt StreamingStrategy;
};

type StreamingCallbackHttpResponse = record {
    body: blob;
    token: opt Token;
};

type Token = record {};

type StreamingStrategy = variant {
    Callback: record {
        callback: func (Token) -> (StreamingCallbackHttpResponse) query;
        token: Token;
    };
};

type Purpose = variant {
    recovery;
    authentication;
};

type KeyType = variant {
    unknown;
    platform;
    cross_platform;
    seed_phrase;
    browser_storage_key;
};

// This describes whether a device is protected or not.
// When protected, a device can only be updated or removed if the
// user is authenticated with that very device.
type DeviceProtection = variant {
    protected;
    unprotected;
};

type Challenge = record {
    png_base64: text;
    challenge_key: ChallengeKey;
};

type DeviceData = record {
    pubkey : DeviceKey;
    alias : text;
    credential_id : opt CredentialId;
    purpose: Purpose;
    key_type: KeyType;
    protection: DeviceProtection;
    origin: opt text;
    // Metadata map for additional device information.
    //
    // Note: some fields above will be moved to the metadata map in the future.
    // All field names of `DeviceData` (such as 'alias', 'origin, etc.) are
    // reserved and cannot be written.
    // In addition, the keys usage and authenticator_attachment are reserved as well.
    metadata: opt MetadataMap;
};

// The same as `DeviceData` but with the `last_usage` field.
// This field cannot be written, hence the separate type.
type DeviceWithUsage = record {
    pubkey : DeviceKey;
    alias : text;
    credential_id : opt CredentialId;
    purpose: Purpose;
    key_type: KeyType;
    protection: DeviceProtection;
    origin: opt text;
    last_usage: opt Timestamp;
    metadata: opt MetadataMap;
};

// Map with some variants for the value type.
// Note, due to the Candid mapping this must be a tuple type thus we cannot name the fields `key` and `value`.
type MetadataMap = vec record {
    text;
    variant { map : MetadataMap; string : text; bytes : vec nat8 };
};

type RegisterResponse = variant {
    // A new user was successfully registered.
    registered: record {
        user_number: UserNumber;
    };
    // No more registrations are possible in this instance of the II service canister.
    canister_full;
    // The challenge was not successful.
    bad_challenge;
};

type AddTentativeDeviceResponse = variant {
    // The device was tentatively added.
    added_tentatively: record {
        verification_code: text;
        // Expiration date, in nanos since the epoch
        device_registration_timeout: Timestamp;
    };
    // Device registration mode is off, either due to timeout or because it was never enabled.
    device_registration_mode_off;
    // There is another device already added tentatively
    another_device_tentatively_added;
};

type VerifyTentativeDeviceResponse = variant {
    // The device was successfully verified.
    verified;
    // Wrong verification code entered. Retry with correct code.
    wrong_code: record {
        retries_left: nat8
    };
    // Device registration mode is off, either due to timeout or because it was never enabled.
    device_registration_mode_off;
    // There is no tentative device to be verified.
    no_device_to_verify;
};

type Delegation = record {
    pubkey: PublicKey;
    expiration: Timestamp;
    targets: opt vec principal;
};

type SignedDelegation = record {
    delegation: Delegation;
    signature: blob;
};

type GetDelegationResponse = variant {
    // The signed delegation was successfully retrieved.
    signed_delegation: SignedDelegation;

    // The signature is not ready. Maybe retry by calling `prepare_delegation`
    no_such_delegation
};

type InternetIdentityStats = record {
    users_registered: nat64;
    storage_layout_version: nat8;
    assigned_user_number_range: record {
        nat64;
        nat64;
    };
    archive_info: ArchiveInfo;
    canister_creation_cycles_cost: nat64;
    max_num_latest_delegation_origins: nat64;
    latest_delegation_origins: vec FrontendHostname
};

// Configuration parameters related to the archive.
type ArchiveConfig = record {
    // The allowed module hash of the archive canister.
    // Changing this parameter does _not_ deploy the archive, but enable archive deployments with the
    // corresponding wasm module.
    module_hash : blob;
    // Buffered archive entries limit. If reached, II will stop accepting new anchor operations
    // until the buffered operations are acknowledged by the archive.
    entries_buffer_limit: nat64;
    // The maximum number of entries to be transferred to the archive per call.
    entries_fetch_limit: nat16;
    // Polling interval to fetch new entries from II (in nanoseconds).
    // Changes to this parameter will only take effect after an archive deployment.
    polling_interval_ns: nat64;
};

// Information about the archive.
type ArchiveInfo = record {
    // Canister id of the archive or empty if no archive has been deployed yet.
    archive_canister : opt principal;
    // Configuration parameters related to the II archive.
    archive_config: opt ArchiveConfig;
};

// Rate limit configuration.
// Currently only used for `register`.
type RateLimitConfig = record {
    // Time it takes (in ns) for a rate limiting token to be replenished.
    time_per_token_ns : nat64;
    // How many tokens are at most generated (to accommodate peaks).
    max_tokens: nat64;
};

// Init arguments of II which can be supplied on install and upgrade.
// Setting a value to null keeps the previous value.
type InternetIdentityInit = record {
    // Set lowest and highest anchor
    assigned_user_number_range : opt record {
        nat64;
        nat64;
    };
    // Configuration parameters related to the II archive.
    // Note: some parameters changes (like the polling interval) will only take effect after an archive deployment.
    // See ArchiveConfig for details.
    archive_config: opt ArchiveConfig;
    // Set the amounts of cycles sent with the create canister message.
    // This is configurable because in the staging environment cycles are required.
    // The canister creation cost on mainnet is currently 100'000'000'000 cycles. If this value is higher thant the
    // canister creation cost, the newly created canister will keep extra cycles.
    canister_creation_cycles_cost : opt nat64;
    // Rate limit for the `register` call.
    register_rate_limit : opt RateLimitConfig;
    // Maximum number of latest delegation origins to track.
    // Default: 1000
    max_num_latest_delegation_origins : opt nat64;
    // Maximum number of inflight captchas.
    // Default: 500
    max_inflight_captchas: opt nat64;
};

type ChallengeKey = text;

type ChallengeResult = record {
    key : ChallengeKey;
    chars : text;
};

// Extra information about registration status for new devices
type DeviceRegistrationInfo = record {
    // If present, the user has tentatively added a new device. This
    // new device needs to be verified (see relevant endpoint) before
    // 'expiration'.
    tentative_device : opt DeviceData;
    // The timestamp at which the anchor will turn off registration mode
    // (and the tentative device will be forgotten, if any, and if not verified)
    expiration: Timestamp;
};

// Information about the anchor
type IdentityAnchorInfo = record {
    // All devices that can authenticate to this anchor
    devices : vec DeviceWithUsage;
    // Device registration status used when adding devices, see DeviceRegistrationInfo
    device_registration: opt DeviceRegistrationInfo;
};

type AnchorCredentials = record {
    credentials : vec WebAuthnCredential;
    recovery_credentials : vec WebAuthnCredential;
    recovery_phrases: vec PublicKey;
};

type WebAuthnCredential = record {
    credential_id : CredentialId;
    pubkey: PublicKey;
};

type DeployArchiveResult = variant {
    // The archive was deployed successfully and the supplied wasm module has been installed. The principal of the archive
    // canister is returned.
    success: principal;
    // Initial archive creation is already in progress.
    creation_in_progress;
    // Archive deployment failed. An error description is returned.
    failed: text;
};

type BufferedArchiveEntry = record {
    anchor_number: UserNumber;
    timestamp: Timestamp;
    sequence_number: nat64;
    entry: blob;
};

// API V2 specific types
// WARNING: These type are experimental and may change in the future.

type IdentityNumber = nat64;

// Authentication method using WebAuthn signatures
// See https://www.w3.org/TR/webauthn-2/
// This is a separate type because WebAuthn requires to also store
// the credential id (in addition to the public key).
type WebAuthn = record {
    credential_id: CredentialId;
    pubkey: PublicKey;
};

// Authentication method using generic signatures
// See https://internetcomputer.org/docs/current/references/ic-interface-spec/#signatures for
// supported signature schemes.
type PublicKeyAuthn = record {
    pubkey: PublicKey;
};

// The authentication methods currently supported by II.
type AuthnMethod = variant {
    webauthn: WebAuthn;
    pubkey: PublicKeyAuthn;
};

// This describes whether an authentication method is protected or not.
// When protected, a authentication method can only be updated or removed if the
// user is authenticated with that very authentication method.
type AuthnMethodProtection = variant {
    protected;
    unprotected;
};

type AuthnMethodData = record {
    authn_method: AuthnMethod;
    protection: AuthnMethodProtection;
    purpose: Purpose;
    // contains the following fields of the DeviceWithUsage type:
    // - alias
    // - origin
    // - authenticator_attachment: data taken from key_type and reduced to platform, cross_platform or absent on migration
    // - usage: data taken from key_type and reduced to recovery_phrase, browser_storage_key or absent on migration
    // Note: for compatibility reasons with the v1 API, the entries above (if present)
    // must be of the `string` variant. This restriction may be lifted in the future.
    metadata: MetadataMap;
    last_authentication: opt Timestamp;
};

// Extra information about registration status for new authentication methods
type AuthnMethodRegistrationInfo = record {
    // If present, the user has registered a new authentication method. This
    // new authentication needs to be verified before 'expiration' in order to
    // be added to the identity.
    authn_method : opt AuthnMethodData;
    // The timestamp at which the identity will turn off registration mode
    // (and the authentication method will be forgotten, if any, and if not verified)
    expiration: Timestamp;
};

type IdentityInfo = record {
    authn_methods: vec AuthnMethodData;
    authn_method_registration: opt AuthnMethodRegistrationInfo;
    // Authentication method independent metadata
    metadata: MetadataMap;
};

type IdentityInfoResponse = variant {
    ok: IdentityInfo;
};

type AuthnMethodAddResponse = variant {
    ok;
    invalid_metadata: text;
};

type AuthnMethodRemoveResponse = variant {
    ok;
};

type IdentityMetadataReplaceResponse = variant {
    ok;
};

service : (opt InternetIdentityInit) -> {
    init_salt: () -> ();
    create_challenge : () -> (Challenge);
    register : (DeviceData, ChallengeResult, opt principal) -> (RegisterResponse);
    add : (UserNumber, DeviceData) -> ();
    update : (UserNumber, DeviceKey, DeviceData) -> ();
    // Atomically replace device matching the device key with the new device data
    replace : (UserNumber, DeviceKey, DeviceData) -> ();
    remove : (UserNumber, DeviceKey) -> ();
    // Returns all devices of the user (authentication and recovery) but no information about device registrations.
    // Note: Clears out the 'alias' fields on the devices. Use 'get_anchor_info' to obtain the full information.
    // Deprecated: Use 'get_anchor_credentials' instead.
    lookup : (UserNumber) -> (vec DeviceData) query;
    get_anchor_credentials : (UserNumber) -> (AnchorCredentials) query;
    get_anchor_info : (UserNumber) -> (IdentityAnchorInfo);
    get_principal : (UserNumber, FrontendHostname) -> (principal) query;
    stats : () -> (InternetIdentityStats) query;

    enter_device_registration_mode : (UserNumber) -> (Timestamp);
    exit_device_registration_mode : (UserNumber) -> ();
    add_tentative_device : (UserNumber, DeviceData) -> (AddTentativeDeviceResponse);
    verify_tentative_device : (UserNumber, verification_code: text) -> (VerifyTentativeDeviceResponse);

    prepare_delegation : (UserNumber, FrontendHostname, SessionKey, maxTimeToLive : opt nat64) -> (UserKey, Timestamp);
    get_delegation: (UserNumber, FrontendHostname, SessionKey, Timestamp) -> (GetDelegationResponse) query;

    http_request: (request: HttpRequest) -> (HttpResponse) query;
    http_request_update: (request: HttpRequest) -> (HttpResponse);

    deploy_archive: (wasm: blob) -> (DeployArchiveResult);
    /// Returns a batch of entries _sorted by sequence number_ to be archived.
    /// This is an update call because the archive information _must_ be certified.
    /// Only callable by this IIs archive canister.
    fetch_entries: () -> (vec BufferedArchiveEntry);
    acknowledge_entries: (sequence_number: nat64) -> ();

    // V2 API
    // WARNING: The following methods are experimental and may change in the future.
    //
    // Note: the responses of v2 API calls are `opt` for compatibility reasons
    // with future variant extensions.
    // A client decoding a response as `null` indicates outdated type information
    // and should be treated as an error.

    // Returns information about the identity with the given number.
    // Requires authentication.
    identity_info: (IdentityNumber) -> (opt IdentityInfoResponse);

    // Replaces the authentication method independent metadata map.
    // The existing metadata map will be overwritten.
    // Requires authentication.
    identity_metadata_replace: (IdentityNumber, MetadataMap) -> (opt IdentityMetadataReplaceResponse);

    // Adds a new authentication method to the identity.
    // Requires authentication.
    authn_method_add: (IdentityNumber, AuthnMethodData) -> (opt AuthnMethodAddResponse);

    // Removes the authentication method associated with the public key from the identity.
    // Requires authentication.
    authn_method_remove: (IdentityNumber, PublicKey) -> (opt AuthnMethodRemoveResponse);
}
    ";
/*
    #[test]
    fn cbor_decode_encode() {
        let res = hex::decode("D9D9F7A167636F6E74656E74A66361726759016B4449444C056D7B6C02007101716D016E7A6C05EFD6E40271E1EDEB4A71A2F5ED880400C6A4A1980602B0F1B99806030104282F5F6170702F696D6D757461626C652F6173736574732F4C6F676F2E66343530363237352E63737303474554000704486F73740B6E6E732E6963302E6170700661636365707412746578742F6373732C2A2F2A3B713D302E31097365632D63682D756100107365632D63682D75612D6D6F62696C65023F30127365632D63682D75612D706C6174666F726D0222220A757365722D6167656E74744D6F7A696C6C612F352E30202857696E646F7773204E542031302E303B2057696E36343B2078363429204170706C655765624B69742F3533372E333620284B48544D4C2C206C696B65204765636B6F29204368726F6D652F3131362E302E353834352E313431205361666172692F3533372E33360F4163636570742D456E636F64696E6717677A69702C206465666C6174652C206964656E746974790102006B63616E69737465725F69644A000000000000000801016E696E67726573735F6578706972791B17823E88F636F9006B6D6574686F645F6E616D656C687474705F726571756573746C726571756573745F747970656571756572796673656E6465724104").unwrap();
        let env : Envelope = serde_cbor::from_slice(&res).unwrap();
        let mut serialized_bytes = Vec::new();
        let mut serializer = serde_cbor::Serializer::new(&mut serialized_bytes);
        serializer.self_describe().unwrap();
        env.serialize(&mut serializer).unwrap();
        let env2: Envelope = serde_cbor::from_slice(&serialized_bytes).unwrap();
        println!("{:?}", env);
        println!("{:?}", env2);
    }

    #[test]
    fn request_decode_encode() {
        let QUERY_REQUEST_IDL: IdlConfig = IdlConfig {
            idl: String::from(SW_IDL),
            direction: MessageType::REQUEST,
            method: "http_request".to_string(),
        };
        let CALL_REQUEST_IDL: IdlConfig = IdlConfig {
            idl: String::from(ICRC1_IDL),
            direction: MessageType::REQUEST,
            method: "icrc1_balance_of".to_string(),
        };

        print_cbor_decode_encode(String::from(QUERY_REQUEST), Some(QUERY_REQUEST_IDL));
    }

    #[test]
    fn query_response_decode_encode() {
        let QUERY_REQUEST_IDL: IdlConfig = IdlConfig {
            idl: String::from(SW_IDL),
            direction: MessageType::RESPONSE,
            method: "http_request".to_string(),
        };
        print_cbor_decode_encode_query_response(String::from(QUERY_RESPONSE_REJECTED), None);
    }

    #[test]
    fn readstate_response_decode_encode() {
        let PREP_DELEGATION_IDL: IdlConfig = IdlConfig {
            idl: String::from(II_IDL),
            direction: MessageType::RESPONSE,
            method: "create_challenge".to_string()
        };
        print_cbor_decode_readstate_response(String::from(READ_STATE_RESPONSE_REPLIED), Some(PREP_DELEGATION_IDL));
    }
    #[test]
    fn decode_canister_request_test() {
        let cbor = hex::decode(QUERY_REQUEST).unwrap();
        let res = decode_canister_request(cbor, Some(String::from(SW_IDL))).unwrap();
        match res {
            RequestInfo::Call { .. } => { unreachable!("submitted query") }
            RequestInfo::ReadState { .. } => { unreachable!("submitted query") }
            RequestInfo::Query { decoded_request, .. } => {
                println!("{}", decoded_request);
            }
        }

    }*/
}
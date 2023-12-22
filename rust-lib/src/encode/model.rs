use std::borrow::Cow;
use std::fmt;

use candid::Principal;
use ic_certification::Certificate;
use ic_transport_types::{Delegation, ReadStateResponse, RejectResponse, SignedDelegation};
use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// we cannot use the original types from ic-transport-types because we have to use a different
/// label definition in EnvelopeContent::ReadState.paths that ensures encode(decode(x)) = x
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
            sender_delegation: self.sender_delegation,
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

impl Into<EnvelopeContentPretty> for EnvelopeContent {
    fn into(self) -> EnvelopeContentPretty {
        match self {
            EnvelopeContent::Call { nonce, ingress_expiry, sender, canister_id, method_name, .. } => {
                EnvelopeContentPretty::Call {
                    nonce,
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg: "ARG_PLACEHOLDER".parse().unwrap(),
                }
            }
            EnvelopeContent::Query { ingress_expiry, sender, canister_id, method_name, .. } => {
                EnvelopeContentPretty::Query {
                    ingress_expiry: ingress_expiry,
                    sender: sender,
                    canister_id: canister_id,
                    method_name: method_name,
                    arg: "ARG_PLACEHOLDER".parse().unwrap(),
                }
            }
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
            EnvelopeContentPretty::Call { nonce, ingress_expiry, sender, canister_id, method_name, .. } => {
                EnvelopeContent::Call {
                    nonce,
                    ingress_expiry,
                    sender,
                    canister_id,
                    method_name,
                    arg: vec![],
                }
            }
            EnvelopeContentPretty::Query { ingress_expiry, sender, canister_id, method_name, .. } => {
                EnvelopeContent::Query {
                    ingress_expiry: ingress_expiry,
                    sender: sender,
                    canister_id: canister_id,
                    method_name: method_name,
                    arg: vec![],
                }
            }
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

#[derive(Debug, PartialEq, Eq)]
pub enum RequestMetadata {
    Call {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
        canister_method: String,
    },
    ReadState {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
    },
    Query {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
        canister_method: String,
    },
}

#[derive(Debug, PartialEq, Eq)]
pub enum RequestInfo {
    Call {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
        decoded_request: String,
        canister_method: String,
    },
    ReadState {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
        decoded_request: String,
    },
    Query {
        request_id: Vec<u8>,
        sender_info: RequestSenderInfo,
        decoded_request: String,
        canister_method: String,
    },
}

#[derive(Debug, PartialEq, Eq)]
pub struct RequestSenderInfo {
    pub sender: Principal,
    pub pubkey: Option<Vec<u8>>,
    pub sig: Option<Vec<u8>>,
    pub delegation: Vec<RequestSenderDelegation>,
}

#[derive(Debug, PartialEq, Eq)]
pub struct RequestSenderDelegation {
    pub pubkey: Vec<u8>,
    pub expiration: u64,
    pub targets: Vec<Principal>,
    pub signature: Vec<u8>,
}

impl From<SignedDelegation> for RequestSenderDelegation {
    fn from(del: SignedDelegation) -> Self {
        RequestSenderDelegation {
            pubkey: del.delegation.pubkey,
            expiration: del.delegation.expiration,
            targets: match del.delegation.targets {
                None => { vec![] }
                Some(t) => { t }
            },
            signature: del.signature,
        }
    }
}

impl Into<SignedDelegation> for RequestSenderDelegation {
    fn into(self) -> SignedDelegation {
        SignedDelegation {
            delegation: Delegation {
                pubkey: self.pubkey,
                expiration: self.expiration,
                targets: if self.targets.len() == 0 { None } else { Some(self.targets) },
                senders: None,
            },
            signature: self.signature,
        }
    }
}

#[derive(Debug)]
pub struct CanisterInterfaceInfo {
    pub canister_interface: String,
    pub canister_method: String,
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
            QueryResponse::Replied { signatures, .. } => { QueryResponsePretty::Replied { reply: CallReplyPretty { arg: "ARG_PLACEHOLDER".parse().unwrap() }, signatures } }
            QueryResponse::Rejected(x) => { QueryResponsePretty::Rejected(x) }
        }
    }
}

impl Into<QueryResponse> for QueryResponsePretty {
    fn into(self) -> QueryResponse {
        match self {
            QueryResponsePretty::Replied { signatures, .. } => { QueryResponse::Replied { reply: CallReply { arg: vec![] }, signatures } }
            QueryResponsePretty::Rejected(x) => { QueryResponse::Rejected(x) }
        }
    }
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

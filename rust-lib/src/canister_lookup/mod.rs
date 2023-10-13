use candid::{Decode, Encode};
use ic_agent::Agent;
use ic_agent::AgentError::{LookupPathAbsent, ReplicaError};
use ic_agent::export::Principal;

use crate::canister_lookup::model::{LookupMethod, LookupResult};
use crate::canister_lookup::model::LookupError::RetrievingCanisterMetadataFailed;

pub mod model;

pub async fn discover_canister_interface(canister_id: String, lookup_method: Option<LookupMethod>) -> LookupResult<Option<String>> {
    let agent = Agent::builder().with_url("https://icp-api.io").build()?;
    let canister = Principal::from_text(canister_id)?;
    let (use_metadata, use_method) = match lookup_method {
        None => (true, true),
        Some(LookupMethod::CanisterMetadata) => (true, false),
        Some(LookupMethod::CanisterMethod) => (false, true),
    };

    if use_metadata {
        let res = agent.read_state_canister_metadata(canister, "candid:service").await;
        match res {
            Ok(x) => return Ok(Some(String::from_utf8(x)?)),
            Err(LookupPathAbsent(_)) => (),
            Err(x) => return Err(RetrievingCanisterMetadataFailed(x)),
        }
    }

    if use_method {
        let res = agent.query(&canister, "__get_candid_interface_tmp_hack".to_string()).with_arg(Encode!()?).call().await;
        match res {
            Ok(x) => return Ok(Some(Decode!(&x, String)?)),
            Err(ReplicaError(_)) => (),
            Err(x) => return Err(RetrievingCanisterMetadataFailed(x)),
        }
    }
    Ok(None)
}

#[cfg(test)]
mod tests {
    use crate::canister_lookup::discover_canister_interface;
    use crate::canister_lookup::model::LookupMethod;

    // these tests are ignored by default because they interact with mainnet
    // if some tests start failing look at https://dashboard.internetcomputer.org/canister/<CID> if
    // the assumptions about interface exposal are still correct
    static II_CANISTER_ID: &str = "rdmx6-jaaaa-aaaaa-aaadq-cai";
    static CKBTC_MINTER_CID: &str = "mqygn-kiaaa-aaaar-qaadq-cai";
    static OC_DAPP_CID: &str = "rturd-qaaaa-aaaaf-aabaq-cai";

    #[tokio::test]
    #[ignore]
    async fn get_ii_canister_interface_via_metadata() {
        let res = discover_canister_interface(II_CANISTER_ID.to_string(), Some(LookupMethod::CanisterMetadata)).await;
        assert!(res.unwrap().is_some());
    }

    #[tokio::test]
    #[ignore]
    async fn dont_get_ii_canister_interface_via_method() {
        let res = discover_canister_interface(II_CANISTER_ID.to_string(), Some(LookupMethod::CanisterMethod)).await;
        assert!(res.unwrap().is_none());
    }

    #[tokio::test]
    #[ignore]
    async fn get_minter_canister_interface_via_method() {
        let res = discover_canister_interface(CKBTC_MINTER_CID.to_string(), Some(LookupMethod::CanisterMethod)).await;
        assert!(res.unwrap().is_some());
    }

    #[tokio::test]
    #[ignore]
    async fn dont_get_canister_interface_via_metadata() {
        let res = discover_canister_interface(OC_DAPP_CID.to_string(), Some(LookupMethod::CanisterMetadata)).await;
        assert!(res.unwrap().is_none());
    }
}

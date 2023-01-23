## Interaction with self managed wallet <a id= "self-managed-wallets"></a>

- Interaction with self managed wallet involves:
  - Establish connection with self managed wallet as defined in [ARIES RFC 0023](https://github.com/hyperledger/aries-rfcs/tree/main/features/0023-did-exchange)
  - Issue credential to self managed wallet as defined in [Aries RFC 0453](https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2)
  - Request presentation from self managed wallet as defined in [Aries RFC 0454](https://github.com/hyperledger/aries-rfcs/tree/main/features/0454-present-proof-v2)

- Current limitation:
  - Request presentation from self managed wallet is not implemented yet
  - Credential revocation is not supported for credentials issued to self managed wallet

### Register, Establish Connection and Issue Membership and Bpn Credential
A self managed wallet can be registered on the MIW by giving the `bpn`, `did`, `name`, and an optional `webhookUrl` to inform the requester when the connection reaches the state `Completed` and the Membership and BPN credentials are issued. 

The following instruction will be executed when a self managed wallet is registered:
  - Establish connection between Base wallet DID and the given DID of the self managed wallet
  - Store the connection Id in database
  - If webhookUrl exist then store it with the request Id of the connection in database
  - Set the state of connection and webhook to `Request``
  - When the self managed wallet accepts the connection, then the built websocket connected to Acapy will trigger a function to perform the following steps:
    - Set the connection state to `Completed`
    - If the WebhookUrl exist then send the information to the stored url
    - Trigger the creation of the Membership and BPN credentials which sends a `Credential Offer` to the self managed wallet
    - When the self managed wallet accepts the offer, then the two credentials will be issued by the Catena X wallet.


### Issue Verifiable Credential for Self Managed Wallet
A credential can be sent to the self managed wallet by calling the `issuance-flow` endpoint: This endpoint takes required information to construct the credential, also an optional webhookUrl as input. This call will trigger the application to send a `Credential Offer` to the self managed wallet. When the `Credential Offer` get accepted by the self managed wallet the managed wallet will issue a credential and send it. When the credential is accepted and stored by the self managed wallet the MIW will then set the state to `Done`. If the webhookUrl exist a notification will be sent.

### Request Verifiable Presentation from Self Managed Wallet
  not implemented yet!

### Local Test Steps:
1. Follow the steps in `Steps for initial local deployment and wallet Creation` section in the `README.md` file
1. Import a new postman collection `Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection.json` from `./dev-asset`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and make sure there are no connections. If there are any please delete them using `Remove Connection`
1. From `Managed Identity Wallet` collection run `Register self managed wallets` 
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and copy the `connection_Id` e.g. `716e678c-f329-4baa-be4d-3c68f004a0ef`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Accept Connection` after replacing the connection id in the path e.g. `http://localhost:11001/didexchange/716e678c-f329-4baa-be4d-3c68f004a0ef/accept-request`
1. The Self Managed Wallet will trigger the AcaPy of MIW and the MIW will change the state of the connection to `Completed` and issue 2 Verifiable Credential Offers. Those can be verified by looking at the database
1. To Accept the BPN Credential by the self managed wallet run `Get Records` after changing the connection Id e.g. `http://localhost:11001/issue-credential-2.0/records?connection_id=716e678c-f329-4baa-be4d-3c68f004a0ef`
1. Search for the BPN Credential and then copy its crednetial exchange id `cred_ex-id` e.g. `e55a3a77-d0bb-43d3-a7a3-0f7003798fc0`
1. To Accept the credential offer and send a request run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Send Credential Request` after replacing the cred_ex_id e.g. `http://localhost:11001/issue-credential-2.0/records/e55a3a77-d0bb-43d3-a7a3-0f7003798fc0/send-request`. This step will trigger the AcaPy of MIW to issue a signed credential.
1. To Store the credential run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Store Credential` after replacing the cred_ex_id in the path and giving a unique id for the credential in the body e.g. `http://localhost:11001/issue-credential-2.0/records/e55a3a77-d0bb-43d3-a7a3-0f7003798fc0/store` with body `{"credential_id": "12345678-9999-43d3-a7a3-111111111111" }`. This will trigger AcaPy to set the credential status of DONE and MIW to send a notification to the webhook, that is given with the wallet registration.
1. To Send another Credential Offer you can call `Managed Identity Wallet/Issuance-flow` and repeat the steps 8 to 11

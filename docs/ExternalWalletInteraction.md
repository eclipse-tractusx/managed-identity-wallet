## Interaction with External Wallets <a id= "external-wallets"></a>

- Interaction with external wallet involves:
  - Accept a connection request from an external wallet to a managed wallet as defined in [ARIES RFC 0023](https://github.com/hyperledger/aries-rfcs/tree/main/features/0023-did-exchange)
  - Receive a credential from external wallet as defined in [Aries RFC 0453](https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2)
  - Send a presentation to external wallet as defined in [Aries RFC 0454](https://github.com/hyperledger/aries-rfcs/tree/main/features/0454-present-proof-v2)

- Current limitation:
  - The managed wallets accept all invitations and credentials
  - The managed wallets can issue credentials only to other managed or registered self-managed wallets after a connection is established
  - The issuer must be an Indy DID on the same ledger as the MIW
  - Credential revocation is not supported for credentials issued using the flows
  - Extensible credentials with extra properties are not supported https://www.w3.org/TR/vc-data-model/#extensibility. The only exception is the property `provenanceProof` which is a list of any type

- Note
  - Each wallet uses the same endpoint, so to the outside world, it is not obvious [multiple tenants](https://github.com/hyperledger/aries-cloudagent-python/blob/main/Multitenancy.md#general-concept) are using the same agent. The message routing from the base wallet to sub-wallets is described in this [docs](https://github.com/hyperledger/aries-cloudagent-python/blob/main/Multitenancy.md#message-routing)

### Establish Connection
All managed wallets in AcaPy have a `webhookUrl` as described in the [documentation](https://github.com/hyperledger/aries-cloudagent-python/blob/main/AdminAPI.md#administration-api-webhooks), which is used to notify the MIW to accept connections, credentials and store them.

The following steps are executed to establish the connection:
  - The external wallet sends an invitation request using its public DID and the public DID of the managed wallet
  - The MIW get triggered by the Webhook endpoint, and it triggers AcaPy back to accept the request
  - The external wallet receive the response from the managed wallet and change its state to `completed`
  - The MIW get triggered again by its Webhook and store the connection with state `completed` using the external DIDs of the wallets

### Receive Verifiable Credential from External Wallet
A Credential-Offer is sent from the external wallet using the established connection with the managed wallet. The Credential-Offer is received by the managed wallet, and this triggers the MIW to send a Credential-Request back to the external Wallet. The external wallet issue the credential and send it to the managed wallet and this triggers the MIW again to store the credential which sends `ack` to the external wallet and change the state of the credential exchange to `DONE` 


### Send Verifiable Presentation to external Wallet
  not implemented yet!

### Local Test Steps:
1. Follow the steps in `Steps for initial local deployment and wallet Creation` section in the `README.md` file
1. Import a new postman collection `Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection.json` from `./dev-asset`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and make sure there are no connections. If there are any please delete them using `Remove Connection`
1. From `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Send Connection Request` using the public DID of the managed wallet
1. MIW will accpet the connection, send acknowledgement and store the connection
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Get Connections` and copy the `connection_id` e.g. `716e678c-f329-4baa-be4d-3c68f004a0ef`
1. Run `Test-Acapy-SelfManagedWallet-Or-ExternalWallet/Send Credential` after replacing the `connection_id`, `credentialSubject.id` and `credential.id` in the body 
1. The managed wallet triggers the MIW that accepts the offer, sends the request and then stores the credential when it is issued
1. The verifiable credential will be stored in Database as well as in AcaPy using the credential_id if exists, otherwise the credential_exchange_id

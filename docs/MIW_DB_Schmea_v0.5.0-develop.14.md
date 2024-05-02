# Database Schemas in v0.5.0


### Wallet Table
| Field                 | Type                       | Description                                                |
|-----------------------|----------------------------|------------------------------------------------------------|
| id                    | Long                       | Primary key for the wallet entity.                         |
| name                  | String                     | Name of the wallet.                                        |
| did                   | String                     | Decentralized identifier (DID) associated with the wallet. |
| bpn                   | String                     | Business Partner Number (BPN) associated with the wallet.  |
| algorithm             | String                     | Algorithm used for the wallet.                             |
| didDocument           | DidDocument                | Document containing the DID information.                   |
| verifiableCredentials | List<VerifiableCredential> | List of verifiable credentials associated with the wallet. |


### WalletKey Table
| Field            | Type   | Description                                        |
|------------------|--------|----------------------------------------------------|
| id               | Long   | Primary key for the wallet key entity.             |
| walletId         | Long   | Identifier of the wallet associated with this key. |
| vaultAccessToken | String | Access token used to access the vault.             |
| referenceKey     | String | Reference key associated with this wallet key.     |
| privateKey       | String | Private key associated with this wallet key.       |
| publicKey        | String | Public key associated with this wallet key.        |
| keyId            | String | Identifier for the key.                            |


### Issuer Credential Table
| Field        | Type                 | Description                            |
|--------------|----------------------|----------------------------------------|
| id           | Long                 | Primary key for the credential entity. |
| holderDid    | String               | DID of the credential holder.          |
| issuerDid    | String               | DID of the credential issuer.          |
| type         | String               | Type of the credential.                |
| data         | VerifiableCredential | Data of the credential.                |
| credentialId | String               | Identifier of the credential.          |


### Holder Credential Table
| Field        | Type                 | Description                                 |
|--------------|----------------------|---------------------------------------------|
| id           | Long                 | Primary key for the credential entity.      |
| holderDid    | String               | DID of the credential holder.               |
| issuerDid    | String               | DID of the credential issuer.               |
| type         | String               | Type of the credential.                     |
| data         | VerifiableCredential | Data of the credential.                     |
| credentialId | String               | Identifier of the credential.               |
| selfIssued   | boolean              | Indicates if the credential is self-issued. |
| stored       | boolean              | Indicates if the credential is stored.      |


### MIWBaseEntity Table
| Field        | Type              | Description                                           |
|--------------|-------------------|-------------------------------------------------------|
| createdAt    | Date              | Date indicating when the entity was created.          |
| modifiedAt   | Date              | Date indicating when the entity was last modified.    |
| modifiedFrom | String            | Source from which the entity was last modified.       |


### BaseCredential Table
| Field        | Type                 | Description                                            |
|--------------|----------------------|--------------------------------------------------------|
| holderDid    | String               | DID of the credential holder.                          |
| issuerDid    | String               | DID of the credential issuer.                          |
| type         | String               | Type of the credential.                                |
| data         | VerifiableCredential | Data of the credential.                                |
| credentialId | String               | Identifier of the credential.                          |
| createdAt    | Date                 | Date indicating when the credential was created.       |
| modifiedAt   | Date                 | Date indicating when the credential was last modified. |
| modifiedFrom | String               | Source from which the credential was last modified.    |


# Class Hierarchy Summary:

1. MIWBaseEntity extends BaseEntity
    - Common base entity providing fields for creation and modification timestamps.

2. WalletKey extends MIWBaseEntity
    - Represents a wallet key entity with associated access and reference keys.

3. HoldersCredential extends MIWBaseEntity
    - Represents a credential held by a user, containing information such as issuer, type, and data.

4. IssuersCredential extends MIWBaseEntity
    - Represents a credential issued by an authority, containing information such as holder, type, and data.

5. Wallet extends MIWBaseEntity
    - Represents a wallet entity, including properties like name, DID, and associated verifiable credentials.

6. BaseCredential extends MIWBaseEntity
    - Represents a base credential entity, providing common fields like holder, issuer, type, data, and credential ID.


# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet

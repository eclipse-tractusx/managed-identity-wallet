## Authority Wallet Table
- id: integer (Primary Key)
- did: string (Unique)
- bpn: string (Unique)
- private_key: string -> Reference to Vault Identitfier for Private Key
- public_key: string
- algorithm: string

## Tenant Wallet Table
- id: integer (Primary Key)
- bpn: string (Unique) -> bpn
- algorithm: string --> ED25519
- did: string (Unique)
- did_document: string
- authority_wallet_id: integer (Foreign Key)
- created_at

## Private Key Table
- id: integer (Primary Key)
- private_key_data: string -> Reference to Vault Identitfier for Private Key
- wallet_id: integer (Foreign Key)

## Credential Table
- id: integer (Primary Key)
- type: string
- credential_data: string
- holder_did: integer (Foreign Key) -> did von tenant
- issuer_did: integer

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet

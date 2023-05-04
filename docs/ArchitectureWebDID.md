## Authority Wallet Table
- id: integer (Primary Key)
- did: string (Unique)
- private_key: string -> Reference to Vault Identitfier for Private Key
- public_key: string
- algorithm: string

## Tenant Wallet Table
- id: integer (Primary Key)
- bpn: string (Unique) -> bpn
- algorithm: string
- did: string (Unique)
- did_document: string
- authority_wallet_id: integer (Foreign Key)

## Private Key Table
- id: integer (Primary Key)
- private_key_data: string -> Reference to Vault Identitfier for Private Key
- wallet_id: integer (Foreign Key)

## Credential Table
- id: integer (Primary Key)
- type: string
- credential_data: string
- tenant_wallet_id: integer (Foreign Key)
- external_wallet_id: integer(Foreign Key)
- authority_wallet_id: integer (Foreign Key)

###TBD
## External Wallet Table
- id: integer (Primary Key)
- uuid: string (Unique) -> bpn
- did: string (Unique)
- did_document: string
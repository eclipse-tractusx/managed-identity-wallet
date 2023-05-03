## Authority Wallet Table
- id: integer (Primary Key)
- did: string (Unique)
- private_key: string

## Tenant Wallet Table
- id: integer (Primary Key)
- uuid: string (Unique) -> bpn
- did: string (Unique)
- subdomain: string
- did_document: string
- authority_wallet_id: integer (Foreign Key)

## External Wallet Table
- id: integer (Primary Key)
- uuid: string (Unique) -> bpn
- did: string (Unique)
- did_document: string

## Private Key Table
- id: integer (Primary Key)
- private_key_data: string -> Reference to Vault Identitfier for Private Key
- wallet_id: integer (Foreign Key)

## Credential Table
- id: integer (Primary Key)
- credential_data: string
- tenant_wallet_id: integer (Foreign Key)
- external_wallet_id: integer(Foreign Key)
- authority_wallet_id: integer (Foreign Key)
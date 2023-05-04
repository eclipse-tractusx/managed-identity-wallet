# Database Schemas for intial draft

### Wallet Table
- id: integer (Primary Key)
- bpn: string (Primary Key) -> bpn
- algorithm: string --> ED25519
- did: string (Unique)
- did_document: string
- authority: boolean
- active: boolean
- createdAt: timestamp
- modified_at: timestamp
- modified_from: string (BPN from Accesstoken as Foreign Key)

### KeyReference Table
- id: integer (Primary Key)
- reference_key: string -> Reference to Vault Identitfier for Private Key
- wallet_id: integer (Foreign Key)
- vault_accestoken: string
- created_at: timestamp
- modified_at: timestamp
- modified_from: string (BPN from Accesstoken as Foreign Key)

### Credential Table
- id: integer (Primary Key)
- type: string
- credential_data: string
- issuer_did: integer (Foreign Key)
- holder_did: integer (Foreign Key)
- created_at: timestamp
- modified_at: timestamp
- modified_from: string (BPN from Accesstoken as Foreign Key) 

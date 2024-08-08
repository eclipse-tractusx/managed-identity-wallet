# Database Schemas for intial draft

### Status List Index

- id: integer (Primary Key)
- issuer_bpn_status: varchar(27)
- current_index: varchar(16)
- status_list_credential_id: varchar(256) (Unique)

### Status list credential

- id: integer (Primary Key)
- issuer_bpn: varchar(16) (Unique)
- credential: text
- created_at: timestamp
- modified_at: timestamp

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet

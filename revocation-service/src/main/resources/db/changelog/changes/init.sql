/*
 * *******************************************************************************
 *  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

-- liquibase formatted sql

-- changeset pmanaras:1.0 dbms:postgresql
CREATE TABLE status_list_credential (
  id VARCHAR(256) NOT NULL,
   issuer_bpn VARCHAR(16),
   credential TEXT NOT NULL,
   created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
   modified_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
   CONSTRAINT pk_statuslistcredential PRIMARY KEY (id)
);

CREATE TABLE status_list_index (
  id VARCHAR(256) NOT NULL,
   issuer_bpn_status VARCHAR(27),
   current_index VARCHAR(16),
   status_list_credential_id VARCHAR(256),
   CONSTRAINT pk_statuslistindex PRIMARY KEY (id)
);

ALTER TABLE status_list_index ADD CONSTRAINT uc_statuslistindex_status_list_credential UNIQUE (status_list_credential_id);

ALTER TABLE status_list_index ADD CONSTRAINT FK_STATUSLISTINDEX_ON_STATUS_LIST_CREDENTIAL FOREIGN KEY (status_list_credential_id) REFERENCES status_list_credential (id);

-- changeset pmanaras:1.0  dbms:h2
CREATE TABLE IF NOT EXISTS status_list_credential (
  id VARCHAR(256) NOT NULL,
   issuer_bpn VARCHAR(16),
   credential CLOB NOT NULL,
   created_at TIMESTAMP NOT NULL,
   modified_at TIMESTAMP NOT NULL,
   CONSTRAINT pk_statuslistcredential PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS status_list_index (
  id VARCHAR(256) NOT NULL,
   issuer_bpn_status VARCHAR(27),
   current_index VARCHAR(16),
   status_list_credential_id VARCHAR(256),
   CONSTRAINT pk_statuslistindex PRIMARY KEY (id)
);

ALTER TABLE status_list_index DROP CONSTRAINT IF EXISTS uc_statuslistindex_status_list_credential;
ALTER TABLE status_list_index DROP CONSTRAINT IF EXISTS FK_STATUSLISTINDEX_ON_STATUS_LIST_CREDENTIAL;

ALTER TABLE status_list_index ADD CONSTRAINT uc_statuslistindex_status_list_credential UNIQUE (status_list_credential_id);

ALTER TABLE status_list_index ADD CONSTRAINT FK_STATUSLISTINDEX_ON_STATUS_LIST_CREDENTIAL FOREIGN KEY (status_list_credential_id) REFERENCES status_list_credential (id);

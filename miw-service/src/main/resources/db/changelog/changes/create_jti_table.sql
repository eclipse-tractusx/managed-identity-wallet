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
-- changeset andreibogus:create-jti-table

CREATE TABLE IF NOT EXISTS public.jti
(
    id                bigserial     NOT NULL,
    jti               uuid          NOT NULL,
    is_used_status    bool          NOT NULL,
    created_at    timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at   timestamp(6) NULL,
    modified_from varchar(255) NULL,
    CONSTRAINT jti_pkey PRIMARY KEY (id),
    CONSTRAINT uk_jti UNIQUE (jti)
);
COMMENT ON TABLE public.jti IS 'This table will store jti field statuses';

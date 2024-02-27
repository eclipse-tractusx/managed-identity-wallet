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

package org.eclipse.tractusx.managedidentitywallets.constant;


public enum TokenValidationErrors {

    ACCESS_TOKEN_MISSING,
    ISS_AND_SUB_NOT_EQUAL,
    SUB_NOT_MATCH_ANY_DID,
    SUB_NOT_DID,
    EXP_MISSING,
    TOKEN_ALREADY_EXPIRED,
    IAT_AFTER_EXPIRATION,
    CURRENT_TIME_BEFORE_IAT,
    AUD_MISSING,
    AUD_NOT_DID,
    AUD_CLAIMS_NOT_EQUAL,
    NONCE_MISSING,
    NONCE_CLAIMS_NOT_EQUAL,
    SIGNATURE_NOT_VERIFIED,
    IAT_MISSING
}

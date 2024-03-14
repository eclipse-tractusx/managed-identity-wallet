/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

/**
 * The type Application constant.
 */
public class StringPool {

    public static final String CREDENTIAL_ID = "credentialId";
    public static final String VALUE = "value";
    public static final String CONTRACT_VERSION = "contractVersion";
    public static final String ACTIVITY_TYPE = "activityType";
    public static final String ALLOWED_VEHICLE_BRANDS = "allowedVehicleBrands";
    public static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    public static final String VP = "vp";
    public static final String VALID = "valid";
    public static final String VALIDATE_AUDIENCE = "validateAudience";
    public static final String VALIDATE_EXPIRY_DATE = "validateExpiryDate";
    public static final String VALIDATE_JWT_EXPIRY_DATE = "validateJWTExpiryDate";
    public static final String DID_DOCUMENT = "didDocument";
    public static final String VEHICLE_DISMANTLE = "vehicleDismantle";
    public static final String CREATED_AT = "createdAt";

    private StringPool() {
        throw new IllegalStateException("Constant class");
    }

    public static final String ISSUER_DID = "issuerDid";
    public static final String HOLDER_DID = "holderDid";
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";
    public static final String NAME = "name";
    public static final String CONTRACT_TEMPLATE = "contractTemplate";
    public static final String TYPE = "type";
    public static final String MEMBER_OF = "memberOf";
    public static final String STATUS = "status";
    public static final String START_TIME = "startTime";

    public static final String ED_25519 = "ED25519";


    /**
     * The constant DID.
     */
    public static final String DID = "did";

    /**
     * The constant BPN.
     */
    public static final String BPN = "bpn";

    public static final String BPN_UPPER_CASE = "BPN";

    public static final String ID = "id";

    public static final String ITEMS = "items";


    public static final String CLIENT_ID = "miw_private_client";

    public static final String CLIENT_SECRET = "miw_private_client_secret";

    public static final String REALM = "miw_test";

    public static final String USER_PASSWORD = "s3cr3t";

    public static final String VALID_USER_NAME = "valid_user";

    public static final String INVALID_USER_NAME = "invalid_user";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String OPENID = "openid";
    public static final String BEARER_SPACE = "Bearer ";

    public static final String BPN_NUMBER_REGEX = "^(BPN)(L|S|A)[0-9A-Z]{12}";

    public static final String COMA_SEPARATOR = ", ";
    public static final String BLANK_SEPARATOR = " ";
    public static final String COLON_SEPARATOR = ":";
    public static final String UNDERSCORE = "_";

    public static final String REFERENCE_KEY = "dummy ref key, removed once vault setup is ready";
    public static final String VAULT_ACCESS_TOKEN = "dummy vault access token, removed once vault setup is ready";

    public static final String PRIVATE_KEY = "PRIVATE KEY";
    public static final String PUBLIC_KEY = "PUBLIC KEY";
}

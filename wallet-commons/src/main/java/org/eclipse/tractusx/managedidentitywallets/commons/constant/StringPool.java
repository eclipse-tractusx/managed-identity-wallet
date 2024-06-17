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

package org.eclipse.tractusx.managedidentitywallets.commons.constant;

import lombok.experimental.UtilityClass;

/**
 * The type String pool.
 */
@UtilityClass
public class StringPool {


    /**
     * The constant CREDENTIAL_ID.
     */
    public static final String CREDENTIAL_ID = "credentialId";

    /**
     * The constant VERIFIABLE_CREDENTIALS.
     */
    public static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    /**
     * The constant VP.
     */
    public static final String VP = "vp";
    /**
     * The constant VC.
     */
    public static final String VC = "vc";
    /**
     * The constant VALID.
     */
    public static final String VALID = "valid";
    /**
     * The constant VALIDATE_AUDIENCE.
     */
    public static final String VALIDATE_AUDIENCE = "validateAudience";
    /**
     * The constant VALIDATE_EXPIRY_DATE.
     */
    public static final String VALIDATE_EXPIRY_DATE = "validateExpiryDate";
    /**
     * The constant VALIDATE_JWT_EXPIRY_DATE.
     */
    public static final String VALIDATE_JWT_EXPIRY_DATE = "validateJWTExpiryDate";
    /**
     * The constant DID_DOCUMENT.
     */
    public static final String DID_DOCUMENT = "didDocument";

    /**
     * The constant ISSUER_DID.
     */
    public static final String ISSUER_DID = "issuerDid";
    /**
     * The constant HOLDER_DID.
     */
    public static final String HOLDER_DID = "holderDid";
    /**
     * The constant HOLDER_IDENTIFIER.
     */
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";
    /**
     * The constant TYPE.
     */
    public static final String TYPE = "type";
    /**
     * The constant ED_25519.
     */
    public static final String ED_25519 = "ED25519";


    /**
     * The constant DID.
     */
    public static final String DID = "did";

    /**
     * The constant BPN.
     */
    public static final String BPN = "bpn";

    /**
     * The constant ID.
     */
    public static final String ID = "id";

    /**
     * The constant CLIENT_ID.
     */
    public static final String CLIENT_ID = "miw_private_client";

    /**
     * The constant CLIENT_SECRET.
     */
    public static final String CLIENT_SECRET = "miw_private_client_secret";

    /**
     * The constant REALM.
     */
    public static final String REALM = "miw_test";
    /**
     * The constant VALID_USER_NAME.
     */
    public static final String VALID_USER_NAME = "valid_user";

    /**
     * The constant INVALID_USER_NAME.
     */
    public static final String INVALID_USER_NAME = "invalid_user";
    /**
     * The constant CLIENT_CREDENTIALS.
     */
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    /**
     * The constant OPENID.
     */
    public static final String OPENID = "openid";
    /**
     * The constant BEARER_SPACE.
     */
    public static final String BEARER_SPACE = "Bearer ";

    /**
     * The constant BPN_NUMBER_REGEX.
     */
    public static final String BPN_NUMBER_REGEX = "^(BPN)(L|S|A)[0-9A-Z]{12}";

    /**
     * The constant W3_ID_JWS_2020_V1_CONTEXT_URL.
     */
    public static final String W3_ID_JWS_2020_V1_CONTEXT_URL = "https://w3id.org/security/suites/jws-2020/v1";

    /**
     * The constant COMA_SEPARATOR.
     */
    public static final String COMA_SEPARATOR = ", ";
    /**
     * The constant BLANK_SEPARATOR.
     */
    public static final String BLANK_SEPARATOR = " ";
    /**
     * The constant COLON_SEPARATOR.
     */
    public static final String COLON_SEPARATOR = ":";
    /**
     * The constant UNDERSCORE.
     */
    public static final String UNDERSCORE = "_";

    /**
     * The constant REFERENCE_KEY.
     */
    public static final String REFERENCE_KEY = "dummy ref key, removed once vault setup is ready";
    /**
     * The constant VAULT_ACCESS_TOKEN.
     */
    public static final String VAULT_ACCESS_TOKEN = "dummy vault access token, removed once vault setup is ready";

    /**
     * The constant PRIVATE_KEY.
     */
    public static final String PRIVATE_KEY = "PRIVATE KEY";
    /**
     * The constant PUBLIC_KEY.
     */
    public static final String PUBLIC_KEY = "PUBLIC KEY";
    /**
     * The constant VC_JWT_KEY.
     */
    public static final String VC_JWT_KEY = "jwt";

    /**
     * The constant AS_JWT.
     */
    public static final String AS_JWT = "asJwt";

    /**
     * The constant BPN_CREDENTIAL.
     */
    public static final String BPN_CREDENTIAL = "BpnCredential";

    public static final String ASSERTION_METHOD = "assertionMethod";
    public static final String SERVICE_ENDPOINT = "serviceEndpoint";
    public static final String SERVICE = "service";
    public static final String SECURITY_TOKEN_SERVICE = "SecurityTokenService";
    public static final String CREDENTIAL_SERVICE = "CredentialService";
    public static final String HTTPS_SCHEME = "https://";
    public static final String BPN_NOT_FOUND = "BPN not found";

    /**
     * The constant REVOCABLE.
     */
    public static final String REVOCABLE = "revocable";

    /**
     * The constant CREDENTIAL_STATUS.
     */
    public static final String CREDENTIAL_STATUS = "credentialStatus";

    /**
     * The constant STATUS.
     */
    public static final String STATUS = "status";

}

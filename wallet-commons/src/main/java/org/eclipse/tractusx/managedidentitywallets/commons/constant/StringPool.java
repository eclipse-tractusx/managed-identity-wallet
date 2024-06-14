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

@UtilityClass
public class StringPool {


    public static final String CREDENTIAL_ID = "credentialId";

    public static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    public static final String VP = "vp";
    public static final String VC = "vc";
    public static final String VALID = "valid";
    public static final String VALIDATE_AUDIENCE = "validateAudience";
    public static final String VALIDATE_EXPIRY_DATE = "validateExpiryDate";
    public static final String VALIDATE_JWT_EXPIRY_DATE = "validateJWTExpiryDate";
    public static final String DID_DOCUMENT = "didDocument";

    public static final String ISSUER_DID = "issuerDid";
    public static final String HOLDER_DID = "holderDid";
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";
    public static final String TYPE = "type";
    public static final String ED_25519 = "ED25519";


    /**
     * The constant DID.
     */
    public static final String DID = "did";

    /**
     * The constant BPN.
     */
    public static final String BPN = "bpn";

    public static final String ID = "id";

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

    public static final String W3_ID_JWS_2020_V1_CONTEXT_URL = "https://w3id.org/security/suites/jws-2020/v1";

    public static final String COMA_SEPARATOR = ", ";
    public static final String BLANK_SEPARATOR = " ";
    public static final String COLON_SEPARATOR = ":";
    public static final String UNDERSCORE = "_";

    public static final String REFERENCE_KEY = "dummy ref key, removed once vault setup is ready";
    public static final String VAULT_ACCESS_TOKEN = "dummy vault access token, removed once vault setup is ready";

    public static final String PRIVATE_KEY = "PRIVATE KEY";
    public static final String PUBLIC_KEY = "PUBLIC KEY";
    public static final String VC_JWT_KEY = "jwt";

    public static final String AS_JWT = "asJwt";

    public static final String BPN_CREDENTIAL = "BpnCredential";

    public static final String ASSERTION_METHOD = "assertionMethod";
    public static final String SERVICE_ENDPOINT = "serviceEndpoint";
    public static final String SERVICE = "service";
    public static final String SECURITY_TOKEN_SERVICE = "SecurityTokenService";
    public static final String CREDENTIAL_SERVICE = "CredentialService";
    public static final String HTTPS_SCHEME = "https://";
    public static final String BPN_NOT_FOUND = "BPN not found";

    public static final String REVOCABLE = "revocable";

    public static final String CREDENTIAL_STATUS = "credentialStatus";

}

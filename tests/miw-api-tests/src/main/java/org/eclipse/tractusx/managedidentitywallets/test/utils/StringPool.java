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

package org.eclipse.tractusx.managedidentitywallets.test.utils;

import lombok.experimental.UtilityClass;

/**
 * The type String pool.
 */
@UtilityClass
public class StringPool {

    /**
     * The constant BPN_NUMBER_REGEX.
     */
    public static final String BPN_NUMBER_REGEX = "^(BPN)(L|S|A)[0-9A-Z]{12}";

    /**
     * The constant URI_WALLETS.
     */
    public static final String URI_WALLETS = "/api/wallets";
    /**
     * The constant URI_ISSUE_MEMBERSHIP_VC.
     */
    public static final String URI_ISSUE_MEMBERSHIP_VC = "/api/credentials/issuer/membership";
    /**
     * The constant URI_ISSUE_FRAMEWORK_VC.
     */
    public static final String URI_ISSUE_FRAMEWORK_VC = "/api/credentials/issuer/framework";

    public static final String URI_ISSUER = "/api/credentials/issuer";
    /**
     * The constant URI_ISSUE_DISMANTLER_VC.
     */
    public static final String URI_ISSUE_DISMANTLER_VC = "/api/credentials/issuer/dismantler";
    /**
     * The constant URI_VALIDATE_VC.
     */
    public static final String URI_VALIDATE_VC = "/api/credentials/validation";
    /**
     * The constant URI_CREATE_VP.
     */
    public static final String URI_CREATE_VP = "/api/presentations";
    /**
     * The constant URI_VALIDATE_VP.
     */
    public static final String URI_VALIDATE_VP = "/api/presentations/validation";

    public static final String URI_CREDENTIAL_ISSUER = "/api/credentials/issuer";
    public static final String URI_CREDENTIAL_HOLDER = "/api/credentials";
    /**
     * The constant CONTENT_TYPE_APPLICATION_JSON.
     */
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    public static final String VALIDATE_EXPIRY_DATE = "validateExpiryDate";

    public static final String VALID = "valid";
    public static final String VALIDATE_JWT_EXPIRY_DATE = "validateJWTExpiryDate";
    public static final String AUDIENCE = "audience";
    public static final String AS_JWT = "asJwt";
    public static final String VALIDATE_AUDIENCE = "validateAudience";
    /**
     * The constant BPN.
     */
    public static final String BPN = "bpn";

    /**
     * The constant CONTENT.
     */
    public static final String CONTENT = "content";

    /**
     * The constant CREATED_AT.
     */
    public static final String CREATED_AT = "createdAt";

    /**
     * The constant DID_DOCUMENT.
     */
    public static final String DID_DOCUMENT = "didDocument";

    /**
     * The constant NAME.
     */
    public static final String NAME = "name";

    /**
     * The constant HOLDER_IDENTIFIER.
     */
    public static final String ISSUER_IDENTIFIER = "issuerIdentifier";
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";

    /**
     * The constant ITEMS.
     */
    public static final String ITEMS = "items";

    /**
     * The constant TYPE.
     */
    public static final String TYPE = "type";

    /**
     * The constant PAGE_NUMBER.
     */
    public static final String PAGE_NUMBER = "pageNumber";

    /**
     * The constant SIZE.
     */
    public static final String SIZE = "size";

    /**
     * The constant BPN_CREDENTIAL.
     */
    public static final String BPN_CREDENTIAL = "BpnCredential";

    /**
     * The constant SUMMARY_CREDENTIAL.
     */
    public static final String SUMMARY_CREDENTIAL = "SummaryCredential";

    /**
     * The constant SORT_COLUMN.
     */
    public static final String SORT_COLUMN = "sortColumn";
    public static final String WITH_CREDENTIALS = "withCredentials";
    public static final String WITH_CREDENTIALS_EXPIRY_DATE = "withCredentialExpiryDate";
    public static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    public static final String DID = "did";

    public static final String HOLDER_DID = "holderDid";


    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
}

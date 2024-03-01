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
 * The type Rest uri.
 */
public class RestURI {

    private RestURI() {
        throw new IllegalStateException("constant class");
    }

    /**
     * The constant WALLETS.
     */
    public static final String WALLETS = "/api/wallets";

    /**
     * The constant DID_DOCUMENTS.
     */
    public static final String DID_DOCUMENTS = "/api/didDocuments/{identifier}";
    public static final String DID_RESOLVE = "/{bpn}/did.json";
    /**
     * The constant WALLETS_BY_BPN.
     */
    public static final String API_WALLETS_IDENTIFIER = "/api/wallets/{identifier}";

    /**
     * The constant API_WALLETS_IDENTIFIER_CREDENTIALS.
     */
    public static final String API_WALLETS_IDENTIFIER_CREDENTIALS = "/api/wallets/{identifier}/credentials";
    /**
     * The constant CREDENTIALS.
     */
    public static final String CREDENTIALS = "/api/credentials";


    public static final String CREDENTIALS_VALIDATION = "/api/credentials/validation";


    public static final String ISSUERS_CREDENTIALS = "/api/credentials/issuer";

    /**
     * The constant CREDENTIALS_ISSUER_MEMBERSHIP.
     */
    public static final String CREDENTIALS_ISSUER_MEMBERSHIP = "/api/credentials/issuer/membership";

    /**
     * The constant CREDENTIALS_ISSUER_DISMANTLER.
     */
    public static final String CREDENTIALS_ISSUER_DISMANTLER = "/api/credentials/issuer/dismantler";

    /**
     * The constant API_CREDENTIALS_ISSUER_FRAMEWORK.
     */
    public static final String API_CREDENTIALS_ISSUER_FRAMEWORK = "/api/credentials/issuer/framework";

    public static final String API_PRESENTATIONS = "/api/presentations";
    public static final String API_PRESENTATIONS_VALIDATION = "/api/presentations/validation";
    public static final String API_PRESENTATIONS_IATP = "/api/presentations/iatp";

}

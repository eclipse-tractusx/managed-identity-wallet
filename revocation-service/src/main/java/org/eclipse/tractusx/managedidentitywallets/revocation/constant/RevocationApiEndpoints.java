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

package org.eclipse.tractusx.managedidentitywallets.revocation.constant;

public class RevocationApiEndpoints {

    public static final String REVOCATION_API = "/api/v1/revocations";
    public static final String CREDENTIALS = "/api/credentials";
    public static final String REVOKE = "/revoke";
    public static final String VERIFY = "/verify";
    public static final String STATUS_ENTRY = "/status-entry";
    public static final String CREDENTIALS_BY_ISSUER = "/credentials";
    public static final String CREDENTIALS_STATUS_INDEX =
            CREDENTIALS_BY_ISSUER + "/{issuerBPN}/{status}/{index}";

    private RevocationApiEndpoints() {
        // static
    }
}

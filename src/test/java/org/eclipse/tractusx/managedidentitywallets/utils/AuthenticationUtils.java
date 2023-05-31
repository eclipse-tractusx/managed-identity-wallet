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

package org.eclipse.tractusx.managedidentitywallets.utils;

import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.jetbrains.annotations.NotNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

public class AuthenticationUtils {

    private static final String CLIENT_ID = "miw_private_client";

    private static final String CLIENT_SECRET = "miw_private_client_secret";

    private static final String REALM = "miw_test";

    private static final String USER_PASSWORD = "s3cr3t";

    private static final String VALID_USER_NAME = "valid_user";

    private static final String INVALID_USER_NAME = "invalid_user";

    private static String getValidUserToken(){
        return getJwtToken(VALID_USER_NAME);
    }

    private static String getInvalidUserToken(){
        return getJwtToken(INVALID_USER_NAME);
    }

    public static String getInvalidToken(){
        return UUID.randomUUID().toString();
    }

    @NotNull
    public static HttpHeaders getInvalidUserHttpHeaders() {
        String token = AuthenticationUtils.getInvalidUserToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        return headers;
    }


    @NotNull
    public static HttpHeaders getValidUserHttpHeaders() {
        String token = AuthenticationUtils.getValidUserToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        return headers;
    }


    private static String getJwtToken(String username) {
        Keycloak keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(TestContextInitializer.getAuthServerUrl())
                .realm(REALM)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .username(username)
                .password(USER_PASSWORD)
                .build();
        String access_token = keycloakAdminClient.tokenManager().getAccessToken().getToken();

        return "Bearer " + access_token;
    }

}

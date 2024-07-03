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
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.jetbrains.annotations.NotNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AuthenticationUtils {


    private static String getValidUserToken() {
        return getJwtToken(StringPool.VALID_USER_NAME);
    }

    private static String getValidUserToken(String bpn) {
        return getJwtToken(StringPool.VALID_USER_NAME, bpn);
    }

    private static String getInvalidUserToken() {
        return getJwtToken(StringPool.INVALID_USER_NAME);
    }

    public static String getInvalidToken() {
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
    public static HttpHeaders getValidUserHttpHeaders(String bpn) {
        String token = AuthenticationUtils.getValidUserToken(bpn);
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


    private static String getJwtToken(String username, String bpn) {

        List<String> list = List.of("BPN", "bpn", "bPn"); //Do not add more field here, if you do make sure you change in keycloak realm file
        Random randomizer = new Random();
        String attributeName = list.get(randomizer.nextInt(list.size()));

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(TestContextInitializer.getAuthServerUrl())
                .realm(StringPool.REALM)
                .clientId(StringPool.CLIENT_ID)
                .clientSecret(StringPool.CLIENT_SECRET)
                .grantType(StringPool.CLIENT_CREDENTIALS)
                .scope(StringPool.OPENID)
                .build();

        RealmResource realmResource = keycloak.realm(StringPool.REALM);

        List<UserRepresentation> userRepresentations = realmResource.users().search(username, true);
        UserRepresentation userRepresentation = userRepresentations.get(0);
        UserResource userResource = realmResource.users().get(userRepresentations.get(0).getId());
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        userRepresentation.setAttributes(Map.of(attributeName, List.of(bpn)));
        userResource.update(userRepresentation);
        return getJwtToken(username);
    }

    public static void setupKeycloakClient(String clientId, String clientSecret, String bpn) {
        Keycloak keycloakAdmin = KeycloakBuilder.builder()
                .serverUrl(TestContextInitializer.getAuthServerUrl())
                .realm("master") // Use the master realm for admin operations
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("BPN", bpn);

        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setEnabled(true);
        clientRepresentation.setServiceAccountsEnabled(true);
        clientRepresentation.setClientId(clientId);
        clientRepresentation.setSecret(clientSecret);
        clientRepresentation.setConsentRequired(false);
        clientRepresentation.setAttributes(attributes);

        ProtocolMapperRepresentation propertyMapper = new ProtocolMapperRepresentation();
        propertyMapper.setName("BPN mapper");
        propertyMapper.setProtocol("openid-connect");
        propertyMapper.setProtocolMapper("oidc-hardcoded-claim-mapper");
        propertyMapper.setConfig(Map.of(
                "claim.name", "BPN",
                "user.attribute", "BPN",
                "claim.value", bpn,
                "id.token.claim", "true",
                "access.token.claim", "true",
                "jsonType.label", "String",
                "userinfo.token.claim", "true"
        ));

        // Set the updated list of protocol mappers back to the client representation
        clientRepresentation.setProtocolMappers(List.of(propertyMapper));
        keycloakAdmin.realm(StringPool.REALM).clients().create(clientRepresentation);
    }

    private static String getJwtToken(String username) {

        Keycloak keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(TestContextInitializer.getAuthServerUrl())
                .realm(StringPool.REALM)
                .clientId(StringPool.CLIENT_ID)
                .clientSecret(StringPool.CLIENT_SECRET)
                .username(username)
                .password(StringPool.USER_PASSWORD)
                .build();
        String access_token = keycloakAdminClient.tokenManager().getAccessToken().getToken();

        return StringPool.BEARER_SPACE + access_token;
    }
}

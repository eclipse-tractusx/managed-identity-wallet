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

package org.eclipse.tractusx.managedidentitywallets.config;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.SneakyThrows;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.ServerSocket;
import java.util.Base64;

public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final int port = findFreePort();
    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer().withRealmImportFile("miw-test-realm.json");

    @SneakyThrows
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        KEYCLOAK_CONTAINER.start();
        String authServerUrl = KEYCLOAK_CONTAINER.getAuthServerUrl();
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        TestPropertyValues.of(
                "server.port=" + port,
                "miw.host: localhost:${server.port}",
                "miw.enforceHttps=false",
                "miw.encryptionKey="+ Base64.getEncoder().encodeToString(secretKey.getEncoded()),
                "miw.authorityWalletBpn: BPNL000000000000",
                "miw.authorityWalletName: Test-X",
                "miw.authorityWalletDid: did:web:localhost%3A${server.port}:BPNL000000000000",
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.datasource.username=sa",
                "spring.datasource.password=password",
                "miw.security.auth-server-url=" + authServerUrl,
                "miw.security.clientId=miw_private_client  ",
                "miw.security.auth-url=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/auth",
                "miw.security.token-url=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/token",
                "miw.security.refresh-token-url=${miw.security.token-url}",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=${miw.security.auth-server-url}realms/${miw.security.realm}",
                "spring.security.oauth2.resourceserver.jwk-set-uri=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/certs"
        ).applyTo(applicationContext.getEnvironment());
    }

    public static String getAuthServerUrl() {
        return KEYCLOAK_CONTAINER.getAuthServerUrl();
    }

    @SneakyThrows
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}

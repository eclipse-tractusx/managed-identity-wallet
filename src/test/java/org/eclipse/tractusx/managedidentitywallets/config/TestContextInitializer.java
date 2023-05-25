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
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:15.2");

    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer().withRealmImportFile("miw-test-realm.json");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        postgreSQLContainer.start();
        KEYCLOAK_CONTAINER.start();
        String authServerUrl = KEYCLOAK_CONTAINER.getAuthServerUrl();

        TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword(),
                "miw.security.auth-server-url=" + authServerUrl,
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
}

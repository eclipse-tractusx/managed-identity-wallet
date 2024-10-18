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

package org.eclipse.tractusx.managedidentitywallets.revocation.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.eclipse.tractusx.managedidentitywallets.revocation.config.security.SecurityConfigProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springdoc.core.models.GroupedOpenApi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OpenApiConfigTest {

    private static SecurityConfigProperties securityConfigProperties;

    private static OpenApiConfig openApiconfig;

    @BeforeAll
    public static void beforeAll() {
        securityConfigProperties = Mockito.mock(SecurityConfigProperties.class);
        openApiconfig = new OpenApiConfig(securityConfigProperties);
    }

    @Test
    void testOpenApiSecurityEnabled() {
        when(securityConfigProperties.enabled()).thenReturn(true);
        OpenAPI openAPI = assertDoesNotThrow(() -> openApiconfig.openAPI());

        assertFalse(openAPI.getSecurity().isEmpty());
        openAPI
                .getSecurity()
                .forEach(s -> assertTrue(s.containsKey("Authenticate using access_token")));
    }

    @Test
    void testOpenApiSecurityDisabled() {
        when(securityConfigProperties.enabled()).thenReturn(false);
        OpenAPI openAPI = assertDoesNotThrow(() -> openApiconfig.openAPI());
        assertNull(openAPI.getSecurity());
    }

    @Test
    void testOpenApiDefinition() {
        GroupedOpenApi groupedOpenApi = assertDoesNotThrow(() -> openApiconfig.openApiDefinition());
        assertNotNull(groupedOpenApi);
    }
}

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

package org.eclipse.tractusx.managedidentitywallets.revocation.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomAuthenticationConverterTest {


    @Test
    void shouldConvertSuccessfullyWithAuthorities() {

        Map<String, Object> roles = Map.of("roles", List.of("dei_muda"));
        Map<String, Object> resourceId = Map.of("resourceId", roles);
        Map<String, Object> resourceAccess = Map.of("resource_access", resourceId);

        Jwt jwt =
                new Jwt(
                        "32453453",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(12)),
                        Map.of("kid", "1234"),
                        resourceAccess);

        CustomAuthenticationConverter converter = new CustomAuthenticationConverter("resourceId");
        AbstractAuthenticationToken abstractAuthenticationToken =
                assertDoesNotThrow(() -> converter.convert(jwt));
        assertFalse(abstractAuthenticationToken.getAuthorities().isEmpty());
    }

    @Test
    void shouldConvertSuccessfullyWithoutAuthoritiesWhenRolesMissing() {
        Map<String, Object> resourceId = Map.of("resourceId", Map.of());
        Map<String, Object> resourceAccess = Map.of("resource_access", resourceId);

        Jwt jwt =
                new Jwt(
                        "32453453",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(12)),
                        Map.of("kid", "1234"),
                        resourceAccess);

        CustomAuthenticationConverter converter = new CustomAuthenticationConverter("resourceId");
        AbstractAuthenticationToken abstractAuthenticationToken =
                assertDoesNotThrow(() -> converter.convert(jwt));
        assertTrue(abstractAuthenticationToken.getAuthorities().isEmpty());
    }

    @Test
    void shouldConvertSuccessfullyWithoutAuthoritiesWhenResourceAccessMissing() {
        Map<String, Object> resourceAccess = Map.of("resource_access", Map.of());

        Jwt jwt =
                new Jwt(
                        "32453453",
                        Instant.now(),
                        Instant.now().plus(Duration.ofDays(12)),
                        Map.of("kid", "1234"),
                        resourceAccess);

        CustomAuthenticationConverter converter = new CustomAuthenticationConverter("resourceId");
        AbstractAuthenticationToken abstractAuthenticationToken =
                assertDoesNotThrow(() -> converter.convert(jwt));
        assertTrue(abstractAuthenticationToken.getAuthorities().isEmpty());
    }
}

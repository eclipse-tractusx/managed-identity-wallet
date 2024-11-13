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

package org.eclipse.tractusx.managedidentitywallets.utils;

import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class BpnValidatorTest {

    private BpnValidator bpnValidator;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        bpnValidator = new BpnValidator();
        jwt = Mockito.mock(Jwt.class);
    }

    @Test
    @DisplayName("Validate when bpn claim is present")
    void validateWhenBpnClaimIsPresent() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(StringPool.BPN, "123456");

        when(jwt.getClaims()).thenReturn(claims);

        OAuth2TokenValidatorResult result = bpnValidator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("Validate when bpn claim is not present")
    void validateWhenBpnClaimIsNotPresent() {
        Map<String, Object> claims = new HashMap<>();

        when(jwt.getClaims()).thenReturn(claims);

        OAuth2TokenValidatorResult result = bpnValidator.validate(jwt);

        assertTrue(result.hasErrors());
        assertEquals(bpnValidator.error.getErrorCode(), result.getErrors().iterator().next().getErrorCode());
        assertEquals(bpnValidator.error.getDescription(), result.getErrors().iterator().next().getDescription());
    }

    @Test
    @DisplayName("Validate when bpn claim is present with different case")
    void validateWhenBpnClaimIsPresentWithDifferentCase() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("BPN", "123456");

        when(jwt.getClaims()).thenReturn(claims);

        OAuth2TokenValidatorResult result = bpnValidator.validate(jwt);

        assertFalse(result.hasErrors());
    }
}

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

package org.eclipse.tractusx.managedidentitywallets.revocation.dto;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TokenResponeTest {
    @Test
    void getAndSetAccessToken() {
        // Arrange
        TokenResponse tokenResponse = new TokenResponse();
        String expectedToken = "someAccessToken123";

        // Act
        tokenResponse.setAccessToken(expectedToken);

        // Assert
        String actualToken = tokenResponse.getAccessToken();
        assertEquals(expectedToken, actualToken, "someAccessToken123");
    }

    @Test
    void setAccessTokenWithNullValue() {
        // Arrange
        TokenResponse tokenResponse = new TokenResponse();

        // Act
        tokenResponse.setAccessToken(null);

        // Assert
        String actualToken = tokenResponse.getAccessToken();
        assertNull(actualToken);
    }

    @Test
    void setAccessTokenWithEmptyString() {
        // Arrange
        TokenResponse tokenResponse = new TokenResponse();
        String expectedToken = "";

        // Act
        tokenResponse.setAccessToken(expectedToken);

        // Assert
        String actualToken = tokenResponse.getAccessToken();
        assertEquals(expectedToken, actualToken, "");
    }
}

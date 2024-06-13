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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.BitSetManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialStatusDtoTest {

    @Test
    void validCredentialStatusDto_CreatesSuccessfully() {
        // Arrange
        String validIndex =
                String.valueOf(BitSetManager.BITSET_SIZE / 2); // any valid index within range

        // Act
        CredentialStatusDto dto =
                new CredentialStatusDto(
                        "id",
                        "revocation",
                        validIndex, // this value is within the range [0, BitSetManager.BITSET_SIZE - 1]
                        "statusListCredential",
                        "BitstringStatusListEntry");

        // Assert
        assertNotNull(dto);
        assertEquals("id", dto.id());
        assertEquals("revocation", dto.statusPurpose());
        assertEquals(validIndex, dto.statusListIndex());
        assertEquals("statusListCredential", dto.statusListCredential());
        assertEquals("BitstringStatusListEntry", dto.type());
    }

    @ParameterizedTest
    @ValueSource(ints = { BitSetManager.BITSET_SIZE, -6 })
    void statusListIndexOutOfRange_ThrowsIllegalArgumentException(int value) {
        // Arrange
        String outOfRangeIndex = String.valueOf(value); // one more than the max index

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new CredentialStatusDto(
                            "id", "statusPurpose", outOfRangeIndex, "statusListCredential", "type");
                });
    }

    @Test
    void anyParameterIsBlank_ThrowsValidationException() {

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertFalse(
                validator
                        .validate(
                                new CredentialStatusDto(
                                        "", // id is blank
                                        "revocation",
                                        "0",
                                        "statusListCredential",
                                        "BitstringStatusListEntry"))
                        .isEmpty());

        assertFalse(
                validator
                        .validate(
                                new CredentialStatusDto(
                                        "id",
                                        "revocation",
                                        "0",
                                        "", // statusListCredential is blank
                                        "BitstringStatusListEntry"))
                        .isEmpty());
    }

    @Test
    @DisplayName("statusPurpose is invalid")
    void invalidStatusPurpose_ThrowsIllegalArgumentException() {
        String invalidPurpose = "invalidPurpose";

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new CredentialStatusDto(
                            "id", invalidPurpose, "0", "statusListCredential", "BitstringStatusListEntry");
                });
    }

    @Test
    @DisplayName("type is invalid")
    void invalidType_ThrowsIllegalArgumentException() {
        String invalidType = "invalidType";

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new CredentialStatusDto("id", "revocation", "0", "statusListCredential", invalidType);
                });
    }

    @Test
    @DisplayName("statusPurpose is valid")
    void validStatusPurpose_DoesNotThrowException() {
        String validPurpose = "revocation";

        assertDoesNotThrow(
                () -> {
                    new CredentialStatusDto(
                            "id", validPurpose, "0", "statusListCredential", "BitstringStatusListEntry");
                });
    }

    @Test
    @DisplayName("type is valid")
    void validType_DoesNotThrowException() {
        String validType = "BitstringStatusListEntry";

        assertDoesNotThrow(
                () -> {
                    new CredentialStatusDto("id", "revocation", "0", "statusListCredential", validType);
                });
    }
}

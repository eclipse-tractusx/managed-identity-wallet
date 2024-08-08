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
import org.eclipse.tractusx.managedidentitywallets.commons.constant.RevocationPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusEntryDtoTest {

    @Test
    void validStatusEntryDto_CreatesSuccessfully() {
        // Arrange
        String validPurpose = RevocationPurpose.REVOCATION.name();

        // Act
        StatusEntryDto dto = new StatusEntryDto(validPurpose, "issuerId");

        // Assert
        assertNotNull(dto);
        assertEquals(validPurpose, dto.purpose());
        assertEquals("issuerId", dto.issuerId());
    }

    @Test
    void purposeIsInvalid_ThrowsIllegalArgumentException() {
        // Arrange
        String invalidPurpose = "invalidPurpose";

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new StatusEntryDto(invalidPurpose, "issuerId");
                });
    }

    @Test
    void anyParameterIsBlank_ThrowsValidationException() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // Act & Assert for each field that should not be blank or null
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StatusEntryDto(
                                "", // purpose is blank
                                "issuerId"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StatusEntryDto(
                                "suspension", // purpose is blank
                                "issuerId"));
        assertFalse(
                validator
                        .validate(
                                new StatusEntryDto(
                                        RevocationPurpose.REVOCATION.name(), "" // issuerId is blank
                                ))
                        .isEmpty());
    }
}

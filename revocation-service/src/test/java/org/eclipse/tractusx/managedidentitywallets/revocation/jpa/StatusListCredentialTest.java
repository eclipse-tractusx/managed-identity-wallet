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

package org.eclipse.tractusx.managedidentitywallets.revocation.jpa;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BPN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureJson
class StatusListCredentialTest {

    @MockBean
    ObjectMapper objectMapper;

    @Autowired
    private TestEntityManager entityManager;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    public void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet(); // Initializes the validator
    }

    private VerifiableCredential createVerifiableCredentialTestData(String subjectId) {
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("id", "urn:uuid:" + UUID.randomUUID());
        credentialData.put("issuer", "https://issuer.example.com");
        credentialData.put("issuanceDate", Instant.now().toString());
        credentialData.put("type", List.of("VerifiableCredential", "StatusListCredential"));

        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("id", subjectId);
        subjectData.put("type", "StatusList2021Credential");
        credentialData.put("credentialSubject", subjectData);
        credentialData.put("@context", VerifiableCredential.DEFAULT_CONTEXT.toString());

        return new VerifiableCredential(credentialData);
    }

    @Test
    void testStatusListCredentialPersistence() {
        // Arrange
        VerifiableCredential credential =
                createVerifiableCredentialTestData("urn:uuid:" + UUID.randomUUID());

        StatusListCredential statusListCredential =
                StatusListCredential.builder()
                        .id(BPN + "revocation#1")
                        .issuerBpn(BPN)
                        .credential(credential)
                        .build();

        // Act
        StatusListCredential persistedStatusListCredential =
                entityManager.persistFlushFind(statusListCredential);

        // Assert
        assertNotNull(persistedStatusListCredential);
        assertEquals(BPN + "revocation#1", persistedStatusListCredential.getId());
        assertEquals(BPN, persistedStatusListCredential.getIssuerBpn());
        assertEquals(credential, persistedStatusListCredential.getCredential());
    }

    @Test
    void givenInvalidId_whenSaving_thenValidationFails() {
        // Arrange
        StatusListCredential statusListCredential =
                StatusListCredential.builder()
                        .issuerBpn("") // Invalid issuerId
                        .credential(createVerifiableCredentialTestData("urn:uuid:" + UUID.randomUUID()))
                        .build();

        // Act and Assert
        Set<ConstraintViolation<StatusListCredential>> violations =
                validator.validate(statusListCredential);
        assertThat(violations).isNotEmpty();
        assertThat(violations.toString()).contains("ID cannot be blank");
    }

    @Test
    void givenNullCredential_whenSaving_thenThrowsException() {
        // Arrange
        StatusListCredential statusListCredential =
                StatusListCredential.builder()
                        .id(BPN + "revocation#1")
                        .issuerBpn(BPN)
                        .credential(null) // Null Credential
                        .build();

        // Act and Assert
        Exception exception =
                assertThrows(
                        ConstraintViolationException.class,
                        () -> {
                            entityManager.persistFlushFind(statusListCredential);
                        });
        String expectedMessage = "Credential cannot be null";
        assertThat(exception.getMessage()).contains(expectedMessage);
    }

    @Test
    void givenInvalidCredentialId_whenSaving_thenValidationFails() {
        // Arrange
        VerifiableCredential invalidCredential =
                createVerifiableCredentialTestData(UUID.randomUUID().toString());

        StatusListCredential statusListCredential =
                StatusListCredential.builder().issuerBpn(BPN).credential(invalidCredential).build();

        // Act and Assert
        Set<ConstraintViolation<StatusListCredential>> violations =
                validator.validate(statusListCredential);
        assertThat(violations).isNotEmpty(); // Because the `credential` field validation would fail
    }
}

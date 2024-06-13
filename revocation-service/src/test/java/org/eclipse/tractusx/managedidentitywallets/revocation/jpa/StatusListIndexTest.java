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
import org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BPN;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@AutoConfigureJson
@DataJpaTest
public class StatusListIndexTest {

    @Autowired
    private TestEntityManager entityManager;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    public void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet(); // Initializes the validator
    }

    @Test
    public void whenFieldsAreValid_thenNoConstraintViolationsAndEntityPersists() {
        String id = BPN + "-revocation#1";
        String issuerBpnStatus = BPN + "-revocation";

        StatusListIndex statusListIndex =
                StatusListIndex.builder()
                        .id(id)
                        .issuerBpnStatus(issuerBpnStatus)
                        .currentIndex("123456") // using numeric string only as index
                        .build();

        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);
        assertThat(violations).isEmpty();

        entityManager.persistAndFlush(statusListIndex);

        StatusListIndex found = entityManager.find(StatusListIndex.class, statusListIndex.getId());
        assertThat(found).isNotNull();
        assertThat(found.getIssuerBpnStatus()).isEqualTo(statusListIndex.getIssuerBpnStatus());
        assertThat(found.getCurrentIndex()).isEqualTo(statusListIndex.getCurrentIndex());
    }

    @Test
    public void whenIdIsBlank_thenConstraintViolationOccurs() {
        StatusListIndex statusListIndex =
                StatusListIndex.builder().id(" ").currentIndex("123456").build();
        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);

        assertThat(violations).isNotEmpty();
        assertThat(violations.toString()).contains("ID cannot be blank");
    }

    @Test
    public void whenIssuerBpnStatusIsBlank_thenConstraintViolationOccurs() {
        StatusListIndex statusListIndex =
                StatusListIndex.builder()
                        .issuerBpnStatus(" ")
                        .id(BPN + "-revocation#1")
                        .currentIndex("123456")
                        .build();
        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);

        assertThat(violations).isNotEmpty();
        assertThat(violations.toString()).contains("Issuer BPN with status cannot be blank");
    }

    @Test
    public void whenCurrentIndexIsBlank_thenConstraintViolationOccurs() {
        StatusListIndex statusListIndex =
                StatusListIndex.builder().issuerBpnStatus(TestUtil.BPN).currentIndex(" ").build();
        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);

        assertThat(violations).isNotEmpty();
        assertThat(violations.toString()).contains("Current index cannot be blank");
    }

    @Test
    public void whenCurrentIndexIsNotNumeric_thenConstraintViolationOccurs() {
        String id = BPN + "-revocation#1";
        String issuerBpnStatus = BPN + "-revocation";
        String wrongIndex = "indexABC";

        StatusListIndex statusListIndex =
                StatusListIndex.builder()
                        .id(id)
                        .issuerBpnStatus(issuerBpnStatus)
                        .currentIndex(wrongIndex) // invalid non-numeric currentIndex
                        .build();
        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);

        assertThat(violations).isNotEmpty();
        assertThat(violations.toString()).contains("Current index must be numeric");
    }

    @Test
    public void whenSetInvalidCurrentIndex_thenIllegalArgumentExceptionIsThrown() {
        // Constructing StatusListIndex using the builder pattern
        // with a valid issuerId and leaving currentIndex unset initially
        StatusListIndex statusListIndex =
                StatusListIndex.builder().issuerBpnStatus(BPN + "-revocation").build();

        // Now we attempt to set an invalid (non-numeric) currentIndex
        // using the setCurrentIndex method which includes validation
        assertThrows(IllegalArgumentException.class, () -> statusListIndex.setCurrentIndex("indexABC"));
    }

    @Test
    public void whenFieldsExceedSizeLimit_thenConstraintViolationOccurs() {
        String longIssuerBpnStatus = BPN + "-revocation1";
        String longCurrentIndex =
                "12345".repeat(4); // The repeat count adjusts on the max size of Index
        String id = "normalid".repeat(76); // The repeat count adjusts on the max length of ID
        StatusListIndex statusListIndex =
                StatusListIndex.builder()
                        .issuerBpnStatus(longIssuerBpnStatus)
                        .currentIndex(longCurrentIndex)
                        .id(id)
                        .build();

        Set<ConstraintViolation<StatusListIndex>> violations = validator.validate(statusListIndex);

        assertThat(violations).isNotEmpty();

        assertThat(violations.toString()).contains("ID cannot exceed 256 characters");
        assertThat(violations.toString()).contains("Current index cannot exceed 16 characters");
    }
}

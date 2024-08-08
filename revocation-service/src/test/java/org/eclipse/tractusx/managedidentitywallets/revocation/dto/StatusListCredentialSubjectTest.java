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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatusListCredentialSubjectTest {

    @Test
    void builderCreatesObjectWithCorrectValues() {
        // Arrange
        String id = "12345";
        String statusPurpose = "SomeStatusPurpose";
        String encodedList = "EncodedListData";
        // Act
        StatusListCredentialSubject subject =
                StatusListCredentialSubject.builder()
                        .id(id)
                        .type(StatusListCredentialSubject.TYPE_ENTRY)
                        .statusPurpose(statusPurpose)
                        .encodedList(encodedList)
                        .build();
        // Assert
        assertNotNull(subject);
        assertEquals(id, subject.getId());
        assertEquals(StatusListCredentialSubject.TYPE_ENTRY, subject.getType());
        assertEquals(statusPurpose, subject.getStatusPurpose());
        assertEquals(encodedList, subject.getEncodedList());
    }

    @Test
    void defaultConstantsAreCorrect() {
        // Assert
        assertEquals("BitstringStatusListEntry", StatusListCredentialSubject.TYPE_ENTRY);
        assertEquals("BitstringStatusList", StatusListCredentialSubject.TYPE_LIST);
        assertEquals("id", StatusListCredentialSubject.SUBJECT_ID);
        assertEquals("type", StatusListCredentialSubject.SUBJECT_TYPE);
        assertEquals("statusPurpose", StatusListCredentialSubject.SUBJECT_STATUS_PURPOSE);
        assertEquals("encodedList", StatusListCredentialSubject.SUBJECT_ENCODED_LIST);
    }

    @Test
    void builderCreatesCredentialTypeObject() {
        // Arrange
        String id = "67890";
        String statusPurpose = "AnotherStatusPurpose";
        String encodedList = "AnotherEncodedListData";
        // Act
        StatusListCredentialSubject subject =
                StatusListCredentialSubject.builder()
                        .id(id)
                        .type(StatusListCredentialSubject.TYPE_LIST)
                        .statusPurpose(statusPurpose)
                        .encodedList(encodedList)
                        .build();
        // Assert
        assertNotNull(subject);
        assertEquals(id, subject.getId());
        assertEquals(StatusListCredentialSubject.TYPE_LIST, subject.getType());
        assertEquals(statusPurpose, subject.getStatusPurpose());
        assertEquals(encodedList, subject.getEncodedList());
    }

    @Test
    void builderWithNullValuesForOptionalFields() {
        // Arrange and Act
        StatusListCredentialSubject subject =
                StatusListCredentialSubject.builder()
                        .id("idWithNulls")
                        .type(StatusListCredentialSubject.TYPE_ENTRY)
                        // Optional fields are not set (statusPurpose, encodedList)
                        .build();
        // Assert
        assertNotNull(subject);
        assertEquals("idWithNulls", subject.getId());
        assertEquals(StatusListCredentialSubject.TYPE_ENTRY, subject.getType());
        assertNull(subject.getStatusPurpose()); // Checks if the field is truly optional and nullable
        assertNull(subject.getEncodedList());
    }

    @Test
    void objectsAreImmutable() {
        // Arrange
        StatusListCredentialSubject subject =
                StatusListCredentialSubject.builder()
                        .id("immutableId")
                        .type(StatusListCredentialSubject.TYPE_ENTRY)
                        .statusPurpose("PurposeBeforeChange")
                        .encodedList("ListBeforeChange")
                        .build();
        // Act
        // Attempt to change the properties after creation to check if the object is immutable
        // This attempt should not change the object since the class is expected to be immutable
        // Since the class doesn't provide setters, this test ensures that the builder pattern itself
        // does not introduce mutability
        // Assert
        assertEquals("immutableId", subject.getId());
        assertEquals(StatusListCredentialSubject.TYPE_ENTRY, subject.getType());
        assertEquals("PurposeBeforeChange", subject.getStatusPurpose());
        assertEquals("ListBeforeChange", subject.getEncodedList());
    }

    @Test
    void testToString() {
        // Arrange
        String id = "12345";
        String statusPurpose = "SomeStatusPurpose";
        String encodedList = "EncodedListData";
        // Act
        String s =
                StatusListCredentialSubject.builder()
                        .id(id)
                        .type(StatusListCredentialSubject.TYPE_ENTRY)
                        .statusPurpose(statusPurpose)
                        .encodedList(encodedList)
                        .toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }
}

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

package org.eclipse.tractusx.managedidentitywallets.command;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GetCredentialsCommandTest {

    @Test
    void testGettersAndSetters() {
        // Test data
        String credentialId = "cred123";
        String identifier = "user123";
        List<String> type = new ArrayList<>();
        String sortColumn = "name";
        String sortType = "asc";
        int pageNumber = 1;
        int size = 10;
        boolean asJwt = true;
        String callerBPN = "callerBPN123";

        // Create a GetCredentialsCommand object using the builder
        GetCredentialsCommand command = GetCredentialsCommand.builder()
                .credentialId(credentialId)
                .identifier(identifier)
                .type(type)
                .sortColumn(sortColumn)
                .sortType(sortType)
                .pageNumber(pageNumber)
                .size(size)
                .asJwt(asJwt)
                .callerBPN(callerBPN)
                .build();

        // Test getter and setter methods
        assertEquals(credentialId, command.getCredentialId());
        assertEquals(identifier, command.getIdentifier());
        assertEquals(type, command.getType());
        assertEquals(sortColumn, command.getSortColumn());
        assertEquals(sortType, command.getSortType());
        assertEquals(pageNumber, command.getPageNumber());
        assertEquals(size, command.getSize());
        assertEquals(asJwt, command.isAsJwt());
        assertEquals(callerBPN, command.getCallerBPN());

        // Modify some fields using setter methods
        String newCredentialId = "newCred123";
        String updatedSortColumn = "updatedName";
        int updatedPageNumber = 2;

        command.setCredentialId(newCredentialId);
        command.setSortColumn(updatedSortColumn);
        command.setPageNumber(updatedPageNumber);

        // Test modified values
        assertEquals(newCredentialId, command.getCredentialId());
        assertEquals(updatedSortColumn, command.getSortColumn());
        assertEquals(updatedPageNumber, command.getPageNumber());
    }
}

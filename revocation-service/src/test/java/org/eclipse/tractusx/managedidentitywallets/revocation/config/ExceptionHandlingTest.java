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

import org.eclipse.tractusx.managedidentitywallets.revocation.exception.CredentialAlreadyRevokedException;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionHandlingTest {

    private static final ExceptionHandling exceptionHandling = new ExceptionHandling();

    @Test
    void handleCredentialAlreadyRevokedException() {
        CredentialAlreadyRevokedException credentialAlreadyRevokedException =
                new CredentialAlreadyRevokedException("credential xyz was already revoked");
        ProblemDetail problemDetail =
                exceptionHandling.handleCredentialAlreadyRevokedException(
                        credentialAlreadyRevokedException);

        assertNotNull(problemDetail);
        assertNotNull(problemDetail.getTitle());
        assertEquals("Revocation service error", problemDetail.getTitle());
        assertEquals(HttpStatus.CONFLICT.value(), problemDetail.getStatus());
    }

    @Test
    void handleForbiddenException() {
        ForbiddenException forbiddenException = new ForbiddenException("no!");
        ProblemDetail problemDetail = exceptionHandling.handleForbiddenException(forbiddenException);

        assertNotNull(problemDetail);
        assertNotNull(problemDetail.getTitle());
        assertEquals("ForbiddenException: no!", problemDetail.getTitle());
        assertEquals(HttpStatus.FORBIDDEN.value(), problemDetail.getStatus());
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("illegal");
        ProblemDetail problemDetail =
                exceptionHandling.handleIllegalArgumentException(illegalArgumentException);

        assertNotNull(problemDetail);
        assertNotNull(problemDetail.getTitle());
        assertEquals("IllegalArgumentException: illegal", problemDetail.getTitle());
        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
    }
}

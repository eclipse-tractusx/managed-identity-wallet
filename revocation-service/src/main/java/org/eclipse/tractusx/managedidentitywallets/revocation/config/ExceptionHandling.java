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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.CredentialAlreadyRevokedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionHandling {

    public static final String TIMESTAMP_KEY = "timestamp";

    @ExceptionHandler(CredentialAlreadyRevokedException.class)
    ProblemDetail handleCredentialAlreadyRevokedException(CredentialAlreadyRevokedException e) {
        String errorMsg = e.getMessage();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, errorMsg);
        problemDetail.setTitle("Revocation service error");
        problemDetail.setProperty(TIMESTAMP_KEY, System.currentTimeMillis());
        return problemDetail;
    }


    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbiddenException(ForbiddenException e) {
        String errorMsg = ExceptionUtils.getMessage(e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, errorMsg);
        problemDetail.setTitle(errorMsg);
        problemDetail.setProperty(TIMESTAMP_KEY, System.currentTimeMillis());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgumentException(IllegalArgumentException e) {
        String errorMsg = ExceptionUtils.getMessage(e);
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMsg);
        problemDetail.setTitle(errorMsg);
        problemDetail.setProperty(TIMESTAMP_KEY, System.currentTimeMillis());
        return problemDetail;
    }
}

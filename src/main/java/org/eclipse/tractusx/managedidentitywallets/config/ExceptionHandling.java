/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The type Exception handling.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandling {

    /**
     * The constant TIMESTAMP.
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * Handle wallet not found problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(WalletNotFoundProblem.class)
    ProblemDetail handleWalletNotFoundProblem(WalletNotFoundProblem e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle duplicate wallet problem problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(DuplicateWalletProblem.class)
    ProblemDetail handleDuplicateWalletProblem(DuplicateWalletProblem e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle forbidden exception problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbiddenException(ForbiddenException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle bad data exception problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(BadDataException.class)
    ProblemDetail handleBadDataException(BadDataException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }


    /**
     * Handle validation problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("Invalid data provided");
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        problemDetail.setProperty("errors", handleValidationError(e.getFieldErrors()));
        return problemDetail;
    }

    /**
     * Handle validation problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleValidation(ConstraintViolationException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid data provided");
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        problemDetail.setProperty("errors", exception.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList());
        return problemDetail;
    }

    /**
     * Handle duplicate credential problem problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler({DuplicateCredentialProblem.class, DuplicateSummaryCredentialProblem.class})
    ProblemDetail handleDuplicateCredentialProblem(RuntimeException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle not found credential problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(CredentialNotFoundProblem.class)
    ProblemDetail handleNotFoundCredentialProblem(CredentialNotFoundProblem e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle exception problem detail.
     *
     * @param e the e
     * @return the problem detail
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleException(Exception e) {
        log.error("Error ", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        problemDetail.setTitle(e.getMessage());
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * @param fieldErrors errors
     * @return ResponseEntity with error details
     */
    private Map<String, String> handleValidationError(List<FieldError> fieldErrors) {

        Map<String, String> messages = new HashMap<>();
        fieldErrors.forEach(fieldError -> messages.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return messages;
    }
}

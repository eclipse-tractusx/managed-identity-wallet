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

import com.apicatalog.jsonld.JsonLdError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.tractusx.managedidentitywallets.exception.*;
import org.eclipse.tractusx.ssi.lib.exception.NoVerificationKeyFoundExcpetion;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(e));
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
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
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
     * Handle illegal argument exception problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    /**
     * Handle method argument type mismatch exception problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        problemDetail.setProperty("invalidArgument", exception.getName());
        return problemDetail;
    }

    /**
     * Handle no verification key found exception problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(NoVerificationKeyFoundExcpetion.class)
    ProblemDetail handleNoVerificationKeyFoundException(NoVerificationKeyFoundExcpetion exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }


    /**
     * Handle property reference exception problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handlePropertyReferenceException(PropertyReferenceException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        problemDetail.setProperty("invalidProperty", exception.getPropertyName());
        return problemDetail;
    }

    /**
     * Handle json ld error problem detail.
     *
     * @param exception the exception
     * @return the problem detail
     */
    @ExceptionHandler(JsonLdError.class)
    ProblemDetail handleJsonLdError(JsonLdError exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        problemDetail.setProperty("error", "Can not parse data on JSON-LD");
        return problemDetail;
    }

    @ExceptionHandler(MissingVcTypesException.class)
    ProblemDetail handleMissingVcTypesException(MissingVcTypesException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
        problemDetail.setProperty(TIMESTAMP, System.currentTimeMillis());
        return problemDetail;
    }

    @ExceptionHandler(PermissionViolationException.class)
    ProblemDetail handlePermissionViolationException(PermissionViolationException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ExceptionUtils.getMessage(exception));
        problemDetail.setTitle(ExceptionUtils.getMessage(exception));
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
        ProblemDetail problemDetail;
        if (e.getCause() instanceof JsonLdError) { //in case of invalid context of VC/VP, ssi-lid is giving RuntimeException cause bt JsonLdError, considering as bad data
            problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        } else {
            problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
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

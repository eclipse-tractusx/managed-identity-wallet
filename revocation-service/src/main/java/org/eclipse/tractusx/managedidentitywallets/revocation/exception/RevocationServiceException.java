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

package org.eclipse.tractusx.managedidentitywallets.revocation.exception;

/**
 * Custom exception class to encapsulate service-related exceptions.
 */
public class RevocationServiceException extends Exception {

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *                {@link #getMessage()} method.
     */
    public RevocationServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *                {@link #getMessage()} method.
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public RevocationServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of (cause==null ? null
     * : cause.toString()) (which typically contains the class and detail message of cause). This
     * constructor is useful for exceptions that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public RevocationServiceException(Throwable cause) {
        super(cause);
    }
}

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

package org.eclipse.tractusx.managedidentitywallets.exception;

/**
 * The type Wallet not found problem.
 */
public class CredentialNotFoundProblem extends RuntimeException {

    /**
     * Instantiates a new Wallet not found problem.
     */
    public CredentialNotFoundProblem() {
    }

    /**
     * Instantiates a new Wallet not found problem.
     *
     * @param message the message
     */
    public CredentialNotFoundProblem(String message) {
        super(message);
    }

    /**
     * Instantiates a new Wallet not found problem.
     *
     * @param message the message
     * @param cause   the cause
     */
    public CredentialNotFoundProblem(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Wallet not found problem.
     *
     * @param cause the cause
     */
    public CredentialNotFoundProblem(Throwable cause) {
        super(cause);
    }
}

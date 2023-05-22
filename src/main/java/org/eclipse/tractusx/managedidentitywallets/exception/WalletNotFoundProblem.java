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

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class WalletNotFoundProblem extends AbstractThrowableProblem {

    /**
     * Instantiates a new Duplicate wallet problem.
     *
     * @param bpn the bpn
     */
    public WalletNotFoundProblem(String bpn) {
        super(
                null,
                "Wallet not found",
                Status.NOT_FOUND,
                String.format("Wallet not found with bpn '%s'", bpn));
    }
}

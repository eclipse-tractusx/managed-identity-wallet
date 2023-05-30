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

package org.eclipse.tractusx.managedidentitywallets.constant;

import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;

/**
 * The type Miw verifiable credential type.
 */
public class MIWVerifiableCredentialType extends VerifiableCredentialType {

    /**
     * The constant DISMANTLER_CREDENTIAL_CX.
     */
    public static final String DISMANTLER_CREDENTIAL_CX = "DismantlerCredentialCX";
    public static final String DISMANTLER_CREDENTIAL = "DismantlerCredential";

    /**
     * The constant USE_CASE_FRAMEWORK_CONDITION_CX.
     */
    public static final String USE_CASE_FRAMEWORK_CONDITION_CX = "UseCaseFrameworkConditionCX";

    public static final String BPN_CREDENTIAL = "BpnCredential";

    public static final String BPN_CREDENTIAL_CX = "BpnCredentialCX";

    public static final String MEMBERSHIP_CREDENTIAL_CX = "MembershipCredentialCX";
}

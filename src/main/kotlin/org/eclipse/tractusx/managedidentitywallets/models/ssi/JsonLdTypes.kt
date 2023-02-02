/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets.models.ssi

class JsonLdTypes {
    companion object {
        const val CREDENTIAL_TYPE = "VerifiableCredential"
        const val MEMBERSHIP_TYPE = "MembershipCredential"
        const val BPN_TYPE = "BpnCredential"
        const val NAME_TYPE = "NameCredential"
        const val BANK_ACCOUNT_TYPE = "BankAccountCredential"
        const val ADDRESS_TYPE = "AddressCredential"
        const val LEGAL_FORM_TYPE = "LegalFormCredential"

        fun getBaseWalletCredentialTypes() : List<String>{
            return listOf(
                MEMBERSHIP_TYPE,
                BPN_TYPE,
                NAME_TYPE,
                BANK_ACCOUNT_TYPE,
                ADDRESS_TYPE,
                LEGAL_FORM_TYPE
            )
        }
    }
}

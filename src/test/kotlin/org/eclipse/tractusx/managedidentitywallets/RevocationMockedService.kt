/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets

import org.eclipse.tractusx.managedidentitywallets.models.ssi.CredentialStatus
import org.eclipse.tractusx.managedidentitywallets.models.ssi.LdProofDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import java.util.*

class RevocationMockedService(private val networkIdentifier: String): IRevocationService {

    override suspend fun registerList(profileName: String, issueCredential: Boolean) = UUID.randomUUID().toString()

    override suspend fun addStatusEntry(profileName: String): CredentialStatus {
        return CredentialStatus(
            statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
            credentialType = "StatusList2021Entry",
            statusPurpose = "revocation",
            index = SingletonTestData.credentialIndex.toString(),
            listUrl = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}"
        )
    }

    override suspend fun getStatusListCredentialOfManagedWallet(listName: String): VerifiableCredentialDto {
        return getStatusListCredentialOfUrl("Mocked-URL")
    }

    override suspend fun getStatusListCredentialOfUrl(statusListUrl: String): VerifiableCredentialDto {
        val didOfIssuer = "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}${getIdentifierOfDid(SingletonTestData.baseWalletDID)}"
        return VerifiableCredentialDto(
            id = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}",
            context = listOf( "https://www.w3.org/2018/credentials/v1","https://w3id.org/vc/status-list/2021/v1"),
            type = listOf("VerifiableCredential", "StatusList2021Credential"),
            issuer = didOfIssuer,
            issuanceDate = "2022-09-01T11:59:00Z",
            credentialSubject = mapOf(
                "id" to "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#list",
                "type" to "StatusList2021",
                "statusPurpose" to "revocation",
                "encodedList" to SingletonTestData.encodedList
            ),
            proof = LdProofDto(
                type= "Ed25519Signature2018",
                created = "2022-09-01T11:59:01Z",
                proofPurpose = "assertionMethod",
                verificationMethod = "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}${getIdentifierOfDid(SingletonTestData.baseWalletDID)}#key-1",
                jws ="eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..DwNECMRBYRbnyGrqL16O97rGdLuuZCsDf9Qc6_RLiValwMdRsD9WcrBnWuBAHDIK_EQ8copXgCEWSZLj-RR9DQ"
            )
        )
    }

    override suspend fun revoke(profileName: String, indexOfCredential: Long) { }

    override suspend fun issueStatusListCredentials(profileName: String?, force: Boolean?) { }

    private fun getIdentifierOfDid(did: String): String {
        val elementsOfDid: List<String> = did.split(":")
        return elementsOfDid[elementsOfDid.size - 1]
    }
}

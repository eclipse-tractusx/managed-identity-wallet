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

package org.eclipse.tractusx.managedidentitywallets.services

import foundation.identity.jsonld.JsonLDUtils
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.AccessToken
import org.eclipse.tractusx.managedidentitywallets.models.AddressDto
import org.eclipse.tractusx.managedidentitywallets.models.BPDMConfig
import org.eclipse.tractusx.managedidentitywallets.models.BankAccountDto
import org.eclipse.tractusx.managedidentitywallets.models.BusinessPartnerDataDto
import org.eclipse.tractusx.managedidentitywallets.models.ConnectionDto
import org.eclipse.tractusx.managedidentitywallets.models.LegalAddressDto
import org.eclipse.tractusx.managedidentitywallets.models.LegalFormDto
import org.eclipse.tractusx.managedidentitywallets.models.NameResponse
import org.eclipse.tractusx.managedidentitywallets.models.NotImplementedException
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdTypes
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialRequestWithoutIssuerDto
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class BusinessPartnerDataServiceImpl(
    private val walletService: IWalletService,
    private val bpdmConfig: BPDMConfig,
    private val client: HttpClient
): IBusinessPartnerDataService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    // TODO: notify if issue credentials failed
    override suspend fun pullDataAndUpdateBaseWalletCredentialsAsync(identifier: String?)
    : Deferred<Boolean> = GlobalScope.async {
        val listOfBPNs = if (identifier.isNullOrEmpty()) {
                walletService.getAllBpns()
            } else listOf(walletService.getBpnFromIdentifier(identifier))

        // The endpoints to pull data have a limit of 5000 entities in one call
        val chunksOfBpns = listOfBPNs.chunked(5000)

        chunksOfBpns.forEach { chunkOfBpn ->
            var accessToken = getAccessToken()

            val legalAddressDataResponse = getLegalAddressResponse(listOfBPNs, accessToken.accessToken)
            val legalAddressDataList: List<LegalAddressDto> =
                if (legalAddressDataResponse.status == HttpStatusCode.OK) {
                    Json.decodeFromString(legalAddressDataResponse.readText())
                } else {
                    log.error("Getting Legal Addresses of LegalEntity has thrown " +
                            "an error with details ${legalAddressDataResponse.readText()}")
                    emptyList()
                }
            val legalAddressDataMap: Map<String, LegalAddressDto> = legalAddressDataList.map {
                it.legalEntity to it
            }.toMap()

            val legalEntitiesSearchResponse = getBusinessPartnerSearchResponse(listOfBPNs, accessToken.accessToken)
            val legalEntityDataList: List<BusinessPartnerDataDto> =
                if (legalEntitiesSearchResponse.status == HttpStatusCode.OK) {
                    Json.decodeFromString(legalEntitiesSearchResponse.readText())
                } else {
                    log.error("Getting Business data of LegalEntity has thrown " +
                            "an error with details ${legalEntitiesSearchResponse.readText()}")
                    emptyList()
                }
            val legalEntityDataMap: Map<String, BusinessPartnerDataDto> = legalEntityDataList.map {
                it.bpn to it
            }.toMap()

            chunkOfBpn.forEach { bpn ->
                if (legalEntityDataMap.containsKey(bpn) || legalAddressDataMap.containsKey(bpn)) {
                    val holder = walletService.getWallet(bpn)
                    val connectionId: ConnectionDto? = if (holder.isSelfManaged) {
                        walletService.getConnectionWithBaseWallet(holder.did)
                    } else { null }
                    issueAndUpdateBaseWalletCredentials(
                        holder = holder,
                        businessPartnerData = legalEntityDataMap.get(bpn),
                        pulledAddressOfBpn = legalAddressDataMap.get(bpn),
                        connection = connectionId
                    )
                } else {
                    log.warn("There are no legalEntity or legalAddress associated to Bpn $bpn")
                }
            }
        }
        return@async true
    }

    override suspend fun issueAndStoreBaseWalletCredentialsAsync(
        walletHolderDto: WalletDto,
        type: String,
        data: Any?
    ): Deferred<Boolean> = GlobalScope.async {
        try {
            val vcToIssue = prepareCatenaXCredential(walletHolderDto.bpn, type, data)
            val verifiableCredential: VerifiableCredentialDto = walletService.issueBaseWalletCredential(vcToIssue)
            val issuedVC = toIssuedVerifiableCredentialRequestDto(verifiableCredential)
            if (issuedVC != null) {
                walletService.storeCredential(walletHolderDto.bpn, issuedVC)
                return@async true
            }
            log.error("Error: Proof of Credential of type $type is empty")
            false
        } catch (e: Exception) {
            log.error("Error: Issue Catena-X Credentials of type $type failed with message ${e.message}")
            false
        }
    }

    override suspend fun issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
        targetWallet: WalletDto,
        connectionId: String,
        webhookUrl: String?,
        type: String,
        data: Any?
    ): Deferred<Boolean> =
        GlobalScope.async {
            val baseWalletDid = walletService.getBaseWallet().did
            val credentialFlowRequest: VerifiableCredentialRequestWithoutIssuerDto =
                prepareCatenaXCredential(targetWallet.bpn, type, data)
            val vCIssuanceFlowRequest = VerifiableCredentialIssuanceFlowRequest(
                id =  credentialFlowRequest.id,
                context = credentialFlowRequest.context,
                type = credentialFlowRequest.type,
                issuanceDate = credentialFlowRequest.issuanceDate,
                issuerIdentifier = baseWalletDid,
                expirationDate = credentialFlowRequest.expirationDate,
                credentialSubject = credentialFlowRequest.credentialSubject,
                credentialStatus = null,
                holderIdentifier = credentialFlowRequest.holderIdentifier,
                isRevocable = credentialFlowRequest.isRevocable,
                webhookUrl = webhookUrl,
                connectionId = connectionId
            )
            walletService.triggerCredentialIssuanceFlow(vCIssuanceFlowRequest)
            true
        }

    // ============== Private ==============

    private suspend fun issueAndUpdateBaseWalletCredentials(
        holder: WalletDto,
        businessPartnerData: BusinessPartnerDataDto?,
        pulledAddressOfBpn: LegalAddressDto?,
        connection: ConnectionDto?
    ): Boolean = GlobalScope.async {
        runBlocking {
            val baseWalletCredentialsOfBpn = walletService.getCredentials(
                walletService.getBaseWallet().bpn, holder.bpn, null, null
            )
            if (businessPartnerData != null) {
                // Name Credentials
                val existingNamesData: MutableList<Pair<NameResponse, VerifiableCredentialDto>> =
                    extractDataFromCredentials(baseWalletCredentialsOfBpn, JsonLdTypes.NAME_TYPE)
                businessPartnerData.names.forEach { name ->
                    checkExistencesAndIssueCredential(holder, existingNamesData,
                        JsonLdTypes.NAME_TYPE, name, connection)
                }
                revokeAndDeleteCredentialsAsync(existingNamesData, holder.isSelfManaged).await()

                // Bank Account Credentials
                val existingBankAccountsData: MutableList<Pair<BankAccountDto, VerifiableCredentialDto>> =
                    extractDataFromCredentials(baseWalletCredentialsOfBpn, JsonLdTypes.BANK_ACCOUNT_TYPE)
                businessPartnerData.bankAccounts.forEach { bankAccount ->
                    checkExistencesAndIssueCredential(holder, existingBankAccountsData,
                        JsonLdTypes.BANK_ACCOUNT_TYPE, bankAccount, connection)
                }
                revokeAndDeleteCredentialsAsync(existingBankAccountsData, holder.isSelfManaged).await()

                // Legal Form Credentials
                val existingLegalFormData: MutableList<Pair<LegalFormDto, VerifiableCredentialDto>> =
                    extractDataFromCredentials(baseWalletCredentialsOfBpn, JsonLdTypes.LEGAL_FORM_TYPE)
                if (businessPartnerData.legalForm != null) {
                    checkExistencesAndIssueCredential(holder, existingLegalFormData,
                        JsonLdTypes.LEGAL_FORM_TYPE, businessPartnerData.legalForm, connection)
                }
                revokeAndDeleteCredentialsAsync(existingLegalFormData, holder.isSelfManaged).await()
            }
            // Address Credentials
            if (pulledAddressOfBpn != null) {
                val existingLegalAddressData : MutableList<Pair<AddressDto, VerifiableCredentialDto>> =
                    extractDataFromCredentials(baseWalletCredentialsOfBpn, JsonLdTypes.ADDRESS_TYPE)
                checkExistencesAndIssueCredential(holder, existingLegalAddressData,
                    JsonLdTypes.ADDRESS_TYPE, pulledAddressOfBpn.legalAddress, connection)
                revokeAndDeleteCredentialsAsync(existingLegalAddressData, holder.isSelfManaged).await()
            }
        }
        return@async true
    }.await()

    private suspend fun <T> checkExistencesAndIssueCredential(
        walletOfHolder: WalletDto,
        existingDataFromCredential: MutableList<Pair<T, VerifiableCredentialDto>>,
        type: String,
        data: T,
        connection: ConnectionDto?
    ) {
        val foundData = existingDataFromCredential.firstOrNull { pair -> pair.first == data }
        if (foundData!= null) {
            existingDataFromCredential.remove(foundData)
        } else {
            if (!walletOfHolder.isSelfManaged) {
                issueAndStoreBaseWalletCredentialsAsync(
                    walletHolderDto = walletOfHolder,
                    type = type,
                    data = data
                ).await()
            } else if (connection != null) {
                issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                    walletOfHolder,
                    connectionId = connection.connectionId,
                    webhookUrl = null,
                    type,
                    data
                ).await()
            } else {
                log.warn("Wallet ${walletOfHolder.bpn} is self managed but has no connection")
            }
        }
    }

    private fun prepareCatenaXCredential(
        bpn: String,
        type: String,
        data: Any?
    ): VerifiableCredentialRequestWithoutIssuerDto {
        val currentDateAsString = JsonLDUtils.dateToString(Date.from(Instant.now()))
        var isRevocable = true
        val credentialSubject = when(type) {
            JsonLdTypes.BPN_TYPE -> {
                isRevocable = false
                mutableMapOf(
                    "type" to listOf(JsonLdTypes.BPN_TYPE),
                    "bpn" to bpn
                )
            }
            JsonLdTypes.MEMBERSHIP_TYPE -> {
                mutableMapOf(
                    "type" to listOf(JsonLdTypes.MEMBERSHIP_TYPE),
                    "memberOf" to "Catena-X",
                    "status" to "Active",
                    "startTime" to currentDateAsString
                )
            }
            JsonLdTypes.NAME_TYPE,
            JsonLdTypes.BANK_ACCOUNT_TYPE,
            JsonLdTypes.LEGAL_FORM_TYPE,
            JsonLdTypes.ADDRESS_TYPE -> {
                mapOf(
                    "data" to data!!,
                    "type" to listOf(type)
                )
            }
            else -> {
                throw NotImplementedException("Credential of type $type is not implemented!")
            }
        }
        return VerifiableCredentialRequestWithoutIssuerDto(
            id = "urn:uuid:${UUID.randomUUID()}",
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS
            ),
            type = listOf(type, JsonLdTypes.CREDENTIAL_TYPE),
            issuanceDate = currentDateAsString,
            credentialSubject = credentialSubject,
            holderIdentifier = bpn,
            isRevocable = isRevocable
        )
    }

    private inline fun <reified T> extractDataFromCredentials(
        availableCredentialsForBPN: List<VerifiableCredentialDto>,
        credentialType: String
    ): MutableList<Pair<T, VerifiableCredentialDto>> {
        return try {
            availableCredentialsForBPN.filter { it.type.contains(credentialType) }
                .map {
                    Pair<T, VerifiableCredentialDto>(
                        Json.decodeFromString(JSONObject(it.credentialSubject["data"] as Map<Any?, Any?>).toString()),
                        it
                    )
                }.toMutableList()
        } catch (e: Exception) {
            log.error("Error while extracting data from credential with type $credentialType")
            emptyList<Pair<T, VerifiableCredentialDto>>().toMutableList()
        }
    }

    private suspend fun revokeAndDeleteCredentialsAsync(
        credentialsToRevoke: List<Pair<Any, VerifiableCredentialDto>>,
        isSelfManagedWallet: Boolean
    ): Deferred<Boolean> = GlobalScope.async {
        try {
            credentialsToRevoke.forEach {
                transaction {
                    runBlocking {
                        // TODO The Acapy java library does not support revocation yet
                        if (!isSelfManagedWallet) {
                            walletService.revokeVerifiableCredential(it.second)
                        }
                        walletService.deleteCredential(it.second.id!!)
                    }
                }
            }
            return@async true
        } catch (e: Exception) {
            log.error("Error: Revoke and delete Catena-X Credentials ${e.message}")
            return@async false
        }
    }

    private suspend fun getBusinessPartnerSearchResponse(listOfBPNs: List<String>, accessToken: String): HttpResponse {
        val requestUrl = "${bpdmConfig.url}/api/catena/legal-entities/search"
        return client.post(requestUrl) {
            url(requestUrl)
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(listOfBPNs)
        }
    }

    private suspend fun getLegalAddressResponse(listOfBPNs: List<String>, accessToken: String): HttpResponse {
        val requestUrl = "${bpdmConfig.url}/api/catena/legal-entities/legal-addresses/search"
        return client.post(requestUrl) {
            url(requestUrl)
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(listOfBPNs)
        }
    }

    private suspend fun getAccessToken(): AccessToken {
        val response: HttpResponse = client.submitForm(
            url = bpdmConfig.tokenUrl,
            formParameters = Parameters.build {
                append("client_id", bpdmConfig.clientId)
                append("grant_type", bpdmConfig.grantType)
                append("client_secret", bpdmConfig.clientSecret)
                append("scope", bpdmConfig.scope)
            }
        )
        return Json.decodeFromString(response.readText())
    }

    private fun toIssuedVerifiableCredentialRequestDto(
        vcDto: VerifiableCredentialDto
    ): IssuedVerifiableCredentialRequestDto? {
        if (vcDto.proof != null) {
            return IssuedVerifiableCredentialRequestDto(
                id = vcDto.id,
                type = vcDto.type,
                context = vcDto.context,
                issuer = vcDto.issuer,
                issuanceDate = vcDto.issuanceDate,
                expirationDate = vcDto.expirationDate,
                credentialSubject = vcDto.credentialSubject,
                credentialStatus = vcDto.credentialStatus,
                proof = vcDto.proof
            )
        }
        return null
    }

}

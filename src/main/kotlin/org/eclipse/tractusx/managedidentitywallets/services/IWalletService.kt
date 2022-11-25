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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import io.ktor.client.statement.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CredentialOfferResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Connection
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.hyperledger.aries.api.connection.ConnectionState
import org.slf4j.LoggerFactory

interface IWalletService {

    fun getWallet(identifier: String, withCredentials: Boolean = false): WalletDto

    fun getDidFromBpn(bpn: String): String

    fun getBpnFromDid(did: String): String

    fun getBpnFromIdentifier(identifier: String): String

    fun getAll(): List<WalletDto>

    fun getAllBpns(): List<String>

    suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto

    suspend fun registerSelfManagedWalletAndBuildConnection(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto
    ): SelfManagedWalletResultDto

    suspend fun deleteWallet(identifier: String): Boolean

    fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean

    suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto

    suspend fun issueCatenaXCredential(
        vcCatenaXRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto

    suspend fun triggerCredentialIssuanceFlow(
        vc: VerifiableCredentialIssuanceFlowRequest
    ): CredentialOfferResponse

    suspend fun resolveDocument(identifier: String): DidDocumentDto

    suspend fun registerBaseWallet(verKey: String): Boolean

    suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifiablePresentationDto

    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto>

    suspend fun deleteCredential(credentialId: String): Boolean

    suspend fun addService(identifier: String, serviceDto: DidServiceDto): DidDocumentDto

    suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ): DidDocumentDto

    suspend fun deleteService(identifier: String, id: String): DidDocumentDto

    fun isCatenaXWallet(bpn: String): Boolean

    fun getCatenaXBpn(): String

    suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean = false,
        withRevocationValidation: Boolean
    ): VerifyResponse

    suspend fun issueStatusListCredential(
        profileName: String,
        listCredentialRequestData: ListCredentialRequestData
    ): VerifiableCredentialDto

    suspend fun revokeVerifiableCredential(vc: VerifiableCredentialDto)

    fun setPartnerMembershipIssued(walletDto: WalletDto)

    fun updateConnectionState(connectionId: String, state: ConnectionState)

    fun getConnection(connectionId: String): ConnectionDto

    fun getConnectionWithCatenaX(theirDid: String): ConnectionDto?

    fun subscribeForAriesWS()

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun createWithAcaPyService(
            walletAndAcaPyConfig: WalletAndAcaPyConfig,
            walletRepository: WalletRepository,
            credentialRepository: CredentialRepository,
            utilsService: UtilsService,
            revocationService: IRevocationService,
            webhookService: IWebhookService,
            connectionRepository: ConnectionRepository
        ): IWalletService {
            val acaPyService = IAcaPyService.create(
                walletAndAcaPyConfig = walletAndAcaPyConfig,
                utilsService = utilsService,
                client = HttpClient {
                    expectSuccess = true
                    install(ResponseObserver) {
                        onResponse { response ->
                            log.debug("HTTP status: ${response.status.value}")
                            log.debug("HTTP description: ${response.status.description}")
                        }
                    }
                    HttpResponseValidator {
                        validateResponse { response: HttpResponse ->
                            val statusCode = response.status.value
                            when (statusCode) {
                                in 300..399 -> throw RedirectResponseException(response, response.status.description)
                                in 400..499 -> throw ClientRequestException(response, response.status.description)
                                in 500..599 -> throw ServerResponseException(response, response.status.description)
                            }
                            if (statusCode >= 600) {
                                throw ResponseException(response, response.status.description)
                            }
                        }
                        handleResponseException { cause: Throwable ->
                            when (cause) {
                                is ClientRequestException -> {
                                    if ("already exists." in cause.message) {
                                        throw ConflictException("Aca-Py Error: ${cause.response.status.description}")
                                    }
                                    if ("Unprocessable Entity" in cause.message) {
                                        throw UnprocessableEntityException("Aca-Py Error: ${cause.response.status.description}")
                                    }
                                    throw cause
                                }
                                else -> throw cause
                            }
                        }
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.BODY
                    }
                    install(JsonFeature) {
                        serializer = JacksonSerializer {
                            enable(SerializationFeature.INDENT_OUTPUT)
                            serializationConfig.defaultPrettyPrinter
                            setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        }
                    }
                }
            )
            return AcaPyWalletServiceImpl(acaPyService, walletRepository, credentialRepository,
                utilsService, revocationService, webhookService, connectionRepository)
        }
    }
}

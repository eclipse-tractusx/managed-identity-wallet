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
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.ConflictException
import org.eclipse.tractusx.managedidentitywallets.models.ConnectionDto
import org.eclipse.tractusx.managedidentitywallets.models.InternalServerErrorException
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.NotImplementedException
import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletResultDto
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.eclipse.tractusx.managedidentitywallets.models.WalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidDocumentDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidServiceDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidServiceUpdateRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.InvitationRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.ListCredentialRequestData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialRequestWithoutIssuerDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CredentialOfferResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.slf4j.LoggerFactory

/**
 * The IWalletService interface describes the core methods for managing wallets, issuing and verifying Credentials.
 */
interface IWalletService {

    /**
     * Retrieves the wallet [WalletDto] of given [identifier].
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param withCredentials if credentials are required. Default is false
     * @return [WalletDto] the data of the wallet
     * @throws NotFoundException if the wallet does not exist
     */
    fun getWallet(identifier: String, withCredentials: Boolean = false): WalletDto

    /**
     * Retrieves the extended wallet data [WalletExtendedData] of given Base Wallet.
     * @return [WalletExtendedData] the extended data of the wallet
     */
    fun getBaseWallet(): WalletExtendedData

    /**
     * Retrieves the DID of a given [bpn]
     * @param bpn the BPN of a stored wallet
     * @return the DID as String
     * @throws NotFoundException if the BPN does not exist
     */
    fun getDidFromBpn(bpn: String): String

    /**
     * Retrieves the BPN of a given [did]
     * @param did the DID of a stored wallet
     * @return the DID as String
     * @throws NotFoundException if the BPN does not exist
     */
    fun getBpnFromDid(did: String): String

    /**
     * Retrieves the BPN of a given [identifier]
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @return the DID as String
     * @throws NotFoundException if the BPN does not exist
     */
    fun getBpnFromIdentifier(identifier: String): String

    /**
     * Retrieves all stored wallets
     * @return A [List] of stored wallets [WalletDto]
     */
    fun getAll(): List<WalletDto>

    /**
     * Retrieves all BPNs of stored wallets
     * @return list of BPNs as String
     */
    fun getAllBpns(): List<String>

    /**
     * Creates a managed wallet giving its BPN and Name.
     * The method create a sub wallet with a DID, register the DID on Ledger,
     * create a revocation-list, trigger setup connection,
     * set endpoint and exchange credential with Base Wallet.
     * @param [walletCreateDto] The wallet to create
     * @return [WalletDto] the data of created wallet
     */
    suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto

    /**
     * Registers a self-managed wallet, build connection and trigger credential exchange
     * @return [SelfManagedWalletResultDto] the data of the self-managed wallet
     */
    suspend fun registerSelfManagedWalletAndBuildConnection(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto
    ): SelfManagedWalletResultDto

    /**
     * Deletes a wallet for a given identifier with its connections and credentials.
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @return true if the wallet is deleted successfully
     * @throws NotFoundException if the wallet does not exist
     */
    suspend fun deleteWallet(identifier: String): Boolean

    /**
     * Stores Verifiable Credential for a given wallet.
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param issuedCredential A signed Verifiable Credential
     * @return true if the Verifiable Credential is stored successfully
     * @throws NotFoundException if the wallet does not exist
     */
    fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean

    /**
     * Issues a Verifiable Credential
     * @param vcRequest A Verifiable Credential to modify and sign
     * @return [VerifiableCredentialDto] A signed Verifiable Credential
     * @throws NotFoundException if the wallet of the issuer does not exist
     */
    suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto

    /**
     * Issues a Verifiable Credential using the Base Wallet
     * @param vcBaseWalletRequest A Verifiable Credential to modify and sign
     * @return [VerifiableCredentialDto] A signed Verifiable Credential
     * @throws NotFoundException if the wallet of the issuer does not exist
     */
    suspend fun issueBaseWalletCredential(
        vcBaseWalletRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto

    /**
     * Triggers the Verifiable Credential Issuance Flow.
     * @param vc A Verifiable Credential to modify and sign
     * @return [VerifiableCredentialDto] The sent Verifiable Credential offer
     * @throws NotFoundException if the wallet of the issuer does not exist
     * @throws UnprocessableEntityException if the holder of Verifiable Credential is not defined
     * @throws InternalServerErrorException if there is no valid connection between issuer and holder
     */
    suspend fun triggerCredentialIssuanceFlow(
        vc: VerifiableCredentialIssuanceFlowRequest
    ): CredentialOfferResponse

    /**
     * Retrieves the DID Document of a given identifier.
     * @param identifier the BPN, DID or wallet-id of a stored wallet. Or any valid resolvable DID
     * @return [DidDocumentDto] The DID Document
     */
    suspend fun resolveDocument(identifier: String): DidDocumentDto

    /**
     * Issues a Verifiable Presentation
     * @param vpRequest A Verifiable Presentation to modify and sign
     * @param withCredentialsValidation To validate the Verifiable Credentials in the Presentation.
     * If this set to false, the other validations flags are ignored
     * @param withCredentialsDateValidation To validate the data of the Verifiable Credentials in the Presentation
     * @param withRevocationValidation To validate if any Verifiable Credential is revoked
     * @return [VerifiableCredentialDto] A signed Verifiable Credential
     * @throws NotFoundException if the wallet of the issuer of the Verifiable Presentation (holder) does not exist
     * @throws UnprocessableEntityException if the Verifiable Credential
     * or its Date or revocation status are not valid or the verification failed (depending on the validation flags).
     */
    suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifiablePresentationDto

    /**
     * Retrieves the stored credentials filtered by given parameters
     * @param issuerIdentifier The issuer of the Verifiable Credential
     * @param holderIdentifier The holder of the Verifiable Credential
     * @param type The type of the Verifiable Credential as String
     * @param credentialId The credentialId of the Verifiable Credential
     * @return A filtered [List] of Verifiable Credentials [VerifiableCredentialDto] or empty list
     */
    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto>

    /**
     * Deletes a stored Verifiable Credential by its Id
     * @param credentialId the Id of the Verifiable Credential
     * @return true if the Verifiable Credential is deleted successfully
     * @throws NotFoundException if the Verifiable Credential with given Id does not exist
     */
    suspend fun deleteCredential(credentialId: String): Boolean

    /**
     * Adds a service endpoint to the DID Document
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param serviceDto the Service to add
     * @throws ConflictException if the service already exists
     * @throws NotImplementedException if the Service type is not supported
     */
    suspend fun addService(identifier: String, serviceDto: DidServiceDto)

    /**
     * Updates a service endpoint in the DID Document
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param id the Id of the exiting Service in the DID Document
     * @param serviceUpdateRequestDto the Service to update
     * @throws NotFoundException the target service endpoint does not exist
     * @throws BadRequestException The updated failed
     */
    suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    )

    /**
     * Deletes a service endpoint from the DID Document.
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param id the Id of the exiting Service in the DID Document
     * @return [DidDocumentDto] the DID Document
     * @throws NotImplementedException always, because the Endpoint is not supported yet
     */
    suspend fun deleteService(identifier: String, id: String): DidDocumentDto

    /**
     * Verifies a singed Verifiable Presentation
     * @param vpDto the Verifiable Presentation to verify
     * @param withDateValidation validate the Date of the Verifiable Credentials in the Presentation
     * @param withRevocationValidation verify if the Credentials are not revoked
     * @return [VerifyResponse] The response including the Presentation
     * @throws UnprocessableEntityException if the Presentation is not valid or the validation failed
     */
    suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean = false,
        withRevocationValidation: Boolean
    ): VerifyResponse

    /**
     * Issues a status list credential using the revocation
     * @param profileName the identifier part of the DID of the wallet
     * @param listCredentialRequestData the list id and its subject
     * @return [VerifiableCredentialDto] the signed status list credential
     * @throws BadRequestException If the Verifiable Credential could not be issued
     * @throws NotFoundException If the Wallet of issuer does not exist
     */
    suspend fun issueStatusListCredential(
        profileName: String,
        listCredentialRequestData: ListCredentialRequestData
    ): VerifiableCredentialDto

    /**
     * Revokes a issued Verifiable Credential.
     * @param vc the Verifiable Credential to revoke
     * @throws UnprocessableEntityException If the Verifiable Credential is not revocable or the properties
     * of the credential is not valid
     */
    suspend fun revokeVerifiableCredential(vc: VerifiableCredentialDto)

    /**
     * Sets the partnerMembership property of given wallet as issued. This wallet should have already the Membership and
     * BPN Credential
     * @param walletDto The given wallet
     * @throws NotFoundException if the wallet does not exist
     */
    fun setPartnerMembershipIssued(walletDto: WalletDto)

    /**
     * Updates the connection State
     * @param connectionId the connectionId of the connection
     * @param rfc23State the rfc23State state of the connection as String
     * @throws NotFoundException if the connection does not exist
     */
    fun updateConnectionState(connectionId: String, rfc23State: String)

    /**
     * Retrieves the connection by its connectionId
     * @param connectionId the connectionId of the connection
     * @return [ConnectionDto] the data of the connection or null
     */
    fun getConnection(connectionId: String): ConnectionDto?

    /**
     * Retrieves the connection between Base Wallet and another wallet
     * @param theirDid the DID of the other wallet
     * @return [ConnectionDto] the data of the connection or null
     */
    fun getConnectionWithBaseWallet(theirDid: String): ConnectionDto?

    /**
     * Adds a connection to a wallet.
     * @param connectionId the connectionId
     * @param connectionTargetDid the DID of the target wallet of the connection
     * @param connectionOwnerDid the DID of the owner wallet of the connection
     * @param connectionState the Rfc23State of the connection
     */
    fun addConnection(
        connectionId: String,
        connectionTargetDid: String,
        connectionOwnerDid: String,
        connectionState: String
    )

    /**
     * Creates and initialize the Base Wallet and subscribe to Websocket of AcaPy.
     * @param bpn the BPN of the Base Wallet
     * @param did the DID of the Base Wallet
     * @param verkey the Verkey of the Base Wallet
     * @param name the name of the Base Wallet
     */
    suspend fun initBaseWalletAndSubscribeForAriesWS(bpn: String, did: String, verkey: String, name: String)

    /**
     * Accepts a connection request by wallet
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param connectionRecord the connectionRecord. It includes the connectionId
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun acceptConnectionRequest(identifier: String, connectionRecord: ConnectionRecord)

    /**
     * Accepts the received Verifiable Credential exchange offer.
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param credExRecord the credential exchange record. It includes the credentialExchangeId
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun acceptReceivedOfferVc(identifier: String, credExRecord: V20CredExRecord)

    /**
     * Accepts and Store the received issued Verifiable Credential.
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param credExRecord the credential exchange record. It includes the credentialExchangeId
     * and credExRecord.resolveLDCredential().credential
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun acceptAndStoreReceivedIssuedVc(identifier: String, credExRecord: V20CredExRecord)

    /**
     * Sets the MetaData of the connection by Base Wallet.
     * @param connectionId the connectionId
     * @throws NotFoundException the connection does not exist
     */
    suspend fun setEndorserMetaDataForAcapyConnection(connectionId: String)

    /**
     * Sets the MetaData of the connection by managed wallet.
     * @param walletId the wallet id of the sub wallet
     * @param connectionId the connectionId
     * @throws NotFoundException the wallet or the connection does not exist
     */
    suspend fun setAuthorMetaData(walletId: String, connectionId: String)

    /**
     * Sends a did-exchange invitation
     * @param identifier the BPN, DID or wallet-id of the wallet
     * @param invitationRequestDto the invitation request data
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun sendInvitation(identifier: String, invitationRequestDto: InvitationRequestDto)

    /**
     * Sets the communication service endpoint using the endorsement of Base Wallet.
     * @param walletId the wallet id as String
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun setCommunicationEndpointUsingEndorsement(walletId: String)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        /**
         * Creates the AcaPy Service which implements the IWalletService.
         * The used HTTP client to communicate with AcaPy is configured in this method
         */
        fun createWithAcaPyService(
            walletAndAcaPyConfig: WalletAndAcaPyConfig,
            walletRepository: WalletRepository,
            credentialRepository: CredentialRepository,
            utilsService: UtilsService,
            revocationService: IRevocationService,
            webhookService: IWebhookService,
            connectionRepository: ConnectionRepository
        ): IWalletService {
            val httpClient = HttpClient {
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
                install(HttpTimeout) {
                    requestTimeoutMillis = 30000
                    connectTimeoutMillis = 30000
                    socketTimeoutMillis = 30000
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
            val acaPyService = IAcaPyService.create(
                walletAndAcaPyConfig = walletAndAcaPyConfig,
                utilsService = utilsService,
                client = httpClient
            )
            return AcaPyWalletServiceImpl(
                acaPyService,
                walletRepository,
                credentialRepository,
                utilsService,
                revocationService,
                webhookService,
                connectionRepository
            )
        }
    }

}

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
import org.eclipse.tractusx.managedidentitywallets.models.ForbiddenException
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
 * The IWalletService interface describes the core methods for managing wallets, issuing and verifying credentials.
 */
interface IWalletService {

    /**
     * Retrieves the wallet [WalletDto] of given [identifier].
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param withCredentials if credentials are required. Default is false
     * @return [WalletDto] the data of the wallet
     * @throws NotFoundException if the wallet does not exist
     */
    fun getWallet(identifier: String, withCredentials: Boolean = false): WalletDto

    /**
     * Retrieves the extended wallet data [WalletExtendedData] of the base wallet.
     * @return [WalletExtendedData] the extended data of the wallet
     */
    fun getBaseWallet(): WalletExtendedData

    /**
     * Retrieves the DID of a given [bpn].
     * @param bpn the BPN of a stored wallet
     * @return the DID as String
     * @throws NotFoundException if no DID mapping for the BPN was found
     */
    fun getDidFromBpn(bpn: String): String

    /**
     * Retrieves the BPN of a given [did].
     * @param did the DID of a stored wallet
     * @return the DID as String
     * @throws NotFoundException if no BPN mapping for the given DID was found
     */
    fun getBpnFromDid(did: String): String

    /**
     * Retrieves the BPN of a given [identifier].
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @return the DID as String
     * @throws NotFoundException if no BPN mapping for the given DID was found
     */
    fun getBpnFromIdentifier(identifier: String): String

    /**
     * Retrieves all stored wallets.
     * @return A list of stored wallets [WalletDto]
     */
    fun getAll(): List<WalletDto>

    /**
     * Retrieves all BPNs of stored wallets.
     * @return list of BPNs as String
     */
    fun getAllBpns(): List<String>

    /**
     * Creates and stores a managed wallet giving its BPN and name.
     * @param [walletCreateDto] The wallet to create
     * @return [WalletDto] the data of created wallet
     */
    suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto

    /**
     * Registers a self-managed wallet and sends an invitation request from the base wallet to the self-managed wallet.
     * The exchange of Membership and BPN credentials with the self-managed wallet is implemented in [BaseWalletAriesEventHandler]
     * @return [SelfManagedWalletResultDto] the data of the self-managed wallet
     */
    suspend fun registerSelfManagedWalletAndBuildConnection(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto
    ): SelfManagedWalletResultDto

    /**
     * Deletes a wallet for a given identifier with its connections and credentials.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @return true if the wallet is deleted successfully
     * @throws NotFoundException if the wallet does not exist
     */
    suspend fun deleteWallet(identifier: String): Boolean

    /**
     * Stores a verifiable credential issued for a given wallet.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param issuedCredential A signed verifiable credential
     * @return true if the verifiable credential is stored successfully
     * @throws NotFoundException if the wallet does not exist
     * @throws ForbiddenException if the subject of the credential is not the DID of the given wallet
     */
    fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean

    /**
     * Issues a verifiable credential.
     * @param vcRequest A verifiable credential to modify and sign
     * @return [VerifiableCredentialDto] A signed verifiable credential
     * @throws NotFoundException if the wallet of the issuer does not exist
     */
    suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto

    /**
     * Issues a verifiable credential using the base wallet.
     * @param vcBaseWalletRequest A verifiable credential to modify and sign
     * @return [VerifiableCredentialDto] A signed verifiable credential
     * @throws NotFoundException if the wallet of the issuer does not exist
     */
    suspend fun issueBaseWalletCredential(
        vcBaseWalletRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto

    /**
     * Triggers the verifiable credential Issuance-Flow.
     * @param vc A verifiable credential to modify and sign
     * @return [VerifiableCredentialDto] The sent verifiable credential offer
     * @throws NotFoundException if the wallet of the issuer does not exist
     * @throws UnprocessableEntityException if the holder of verifiable credential is not defined
     * @throws InternalServerErrorException if there is no valid connection between issuer and holder
     */
    suspend fun triggerCredentialIssuanceFlow(
        vc: VerifiableCredentialIssuanceFlowRequest
    ): CredentialOfferResponse

    /**
     * Retrieves the DID document of a given identifier.
     * @param identifier the BPN, DID or wallet-id of a stored wallet. Or any valid resolvable DID
     * @return [DidDocumentDto] The DID document
     */
    suspend fun resolveDocument(identifier: String): DidDocumentDto

    /**
     * Issues a verifiable presentation.
     * @param vpRequest a verifiable presentation to modify and sign
     * @param withCredentialsValidation to validate the verifiable credentials in the presentation.
     * If this set to false, the other validations flags are ignored
     * @param withCredentialsDateValidation to validate the issuance and expiration dates of the verifiable credentials in the presentation
     * @param withRevocationValidation to validate if any of the verifiable credentials is revoked
     * @return [VerifiablePresentationDto] a signed verifiable presentation
     * @throws NotFoundException if the wallet of the issuer of the verifiable presentation (holder) does not exist
     * @throws UnprocessableEntityException if the verifiable credential
     * or its date or revocation status are not valid or the verification failed (depending on the validation flags).
     */
    suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifiablePresentationDto

    /**
     * Retrieves the stored credentials filtered by given parameters.
     * @param issuerIdentifier The issuer of the verifiable credential
     * @param holderIdentifier The holder of the verifiable credential
     * @param type The type of the verifiable credential as String
     * @param credentialId The credentialId of the verifiable credential
     * @return A filtered list of verifiable credential [VerifiableCredentialDto] or empty list
     */
    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto>

    /**
     * Deletes a stored verifiable credential by its Id.
     * @param credentialId the Id of the verifiable credential
     * @return true if the verifiable credential is deleted successfully
     * @throws NotFoundException if the verifiable credential with given Id does not exist
     */
    suspend fun deleteCredential(credentialId: String): Boolean

    /**
     * Adds a service endpoint to the DID document.
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param serviceDto the service to add
     * @throws ConflictException if the service already exists
     * @throws NotImplementedException if the service type is not supported
     */
    suspend fun addService(identifier: String, serviceDto: DidServiceDto)

    /**
     * Updates a service endpoint in the DID document.
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param id the Id of the exiting service in the DID document
     * @param serviceUpdateRequestDto the Service to update
     * @throws NotFoundException the target service endpoint does not exist
     * @throws BadRequestException if the update failed
     */
    suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    )

    /**
     * Deletes a service endpoint from the DID document.
     * @param identifier the BPN, DID or wallet-id of a managed wallet
     * @param id the Id of the exiting Service in the DID document
     * @return [DidDocumentDto] the DID Document
     * @throws NotFoundException the target service endpoint does not exist
     */
    suspend fun deleteService(identifier: String, id: String): DidDocumentDto

    /**
     * Verifies a verifiable presentation.
     * @param vpDto the verifiable presentation to verify
     * @param withDateValidation validate the issuance and expiration dates of the verifiable credentials in the presentation
     * @param withRevocationValidation verify if the credentials are not revoked
     * @return [VerifyResponse] the response including the presentation
     * @throws UnprocessableEntityException if the presentation is not valid or the validation failed
     */
    suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean = false,
        withRevocationValidation: Boolean
    ): VerifyResponse

    /**
     * Issues a status list credential using the revocation service.
     * @param profileName the identifier part of the DID of the wallet
     * @param listCredentialRequestData the list id and its subject
     * @return [VerifiableCredentialDto] the signed status list credential
     * @throws BadRequestException if the verifiable credential could not be issued
     * @throws NotFoundException if the wallet of issuer does not exist
     */
    suspend fun issueStatusListCredential(
        profileName: String,
        listCredentialRequestData: ListCredentialRequestData
    ): VerifiableCredentialDto

    /**
     * Revokes an issued verifiable credential.
     * @param vc the verifiable credential to revoke
     * @throws UnprocessableEntityException if the verifiable credential is not revocable or the properties
     * of the credential are not valid
     */
    suspend fun revokeVerifiableCredential(vc: VerifiableCredentialDto)

    /**
     * Sets the partnerMembership property of given wallet as issued. This means the wallet has the Membership and
     * BPN credential.
     * @param walletDto The given wallet
     * @throws NotFoundException if the wallet does not exist
     */
    fun setPartnerMembershipIssued(walletDto: WalletDto)

    /**
     * Updates the connection state.
     * @param connectionId the connectionId of the connection
     * @param rfc23State the rfc23State state of the connection as String
     * @throws NotFoundException if the connection does not exist
     */
    fun updateConnectionState(connectionId: String, rfc23State: String)

    /**
     * Retrieves the connection by its connectionId.
     * @param connectionId the connectionId of the connection
     * @return [ConnectionDto] the data of the connection or null
     */
    fun getConnection(connectionId: String): ConnectionDto?

    /**
     * Retrieves the connection between base wallet and another wallet.
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
     * Creates and initializes the base wallet and subscribes to Listeners.
     * @param bpn the BPN of the base Wallet
     * @param did the DID of the base Wallet
     * @param verkey the verkey of the base Wallet
     * @param name the name of the base Wallet
     */
    suspend fun initBaseWalletWithListeners(bpn: String, did: String, verkey: String, name: String)

    /**
     * Accepts a connection request by wallet.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param connectionRecord the connectionRecord. It includes the connectionId
     * @throws NotFoundException if the wallet does not exist
     */
    suspend fun acceptConnectionRequest(identifier: String, connectionRecord: ConnectionRecord)

    /**
     * Accepts the received verifiable credential exchange offer.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param credExRecord the credential exchange record. It includes the credentialExchangeId
     * @throws NotFoundException if the wallet does not exist
     */
    suspend fun acceptReceivedOfferVc(identifier: String, credExRecord: V20CredExRecord)

    /**
     * Accepts and stores the received issued verifiable credential.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param credExRecord the credential exchange record. It includes the credentialExchangeId
     * and credExRecord.resolveLDCredential().credential
     * @throws NotFoundException if the wallet does not exist
     */
    suspend fun acceptAndStoreReceivedIssuedVc(identifier: String, credExRecord: V20CredExRecord)

    /**
     * Sets the MetaData of the connection by base wallet.
     * @param connectionId the connectionId
     * @throws NotFoundException the connection does not exist
     */
    suspend fun setEndorserMetaDataForConnection(connectionId: String)

    /**
     * Sets the MetaData of the connection by managed wallet.
     * @param walletId the wallet id of the sub wallet
     * @param connectionId the connectionId
     * @throws NotFoundException the wallet or the connection does not exist
     */
    suspend fun setAuthorMetaData(walletId: String, connectionId: String)

    /**
     * Sends a did-exchange invitation.
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @param invitationRequestDto the invitation request data
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun sendInvitation(identifier: String, invitationRequestDto: InvitationRequestDto)

    /**
     * Sets the communication service endpoint using the endorsement of base wallet.
     * @param walletId the wallet id as String
     * @throws NotFoundException the wallet does not exist
     */
    suspend fun setCommunicationEndpointUsingEndorsement(walletId: String)

    /**
     * Checks if a received connection request to managed wallet is allowed to be processed.
     * @param connection the received connection request [ConnectionRecord]
     * @return true if the connection request is allowed to be processed
     */
    fun validateConnectionRequestForManagedWallets(connection: ConnectionRecord): Boolean

    /**
     * Checks if a received connection request to base wallet is allowed to be processed.
     * @param connection the received connection request [ConnectionRecord]
     * @param bpn the BPN of the requester wallet
     * @return the stored wallet of the requester, null in case of errors
     */
    suspend fun validateConnectionRequestForBaseWallet(connection: ConnectionRecord, bpn: String): WalletDto?

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        /**
         * Creates the AcaPyWalletServiceImpl which implements the IWalletService.
         * The used HTTP client to communicate with AcaPy instances is configured in this method.
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

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

import io.ktor.client.*
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreateSubWallet
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreateWalletTokenResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreatedSubWalletResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidCreate
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidEndpointWithType
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidRegistration
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidRegistrationResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.ResolutionResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.SignRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletList
import org.hyperledger.acy_py.generated.model.TransactionJobs
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord

/**
 * The IAcaPyService interface describes the functionalities that are required
 * for interacting with the AcaPy API to perform various actions.
 */
interface IAcaPyService {

    /**
     * Retrieves the Configuration of Base Wallet and AcaPy.
     * @return [WalletAndAcaPyConfig] with secrets data
     */
    fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig

    /**
     * Retrieves a List of created sub-wallets from Multi-tenancy AcaPy Instance.
     * @return [WalletList] List of wallet records
     * @throws BadRequestException if the get request failed
     */
    suspend fun getSubWallets(): WalletList

    /**
     * Creates a new sub-wallet.
     * @param subWallet the data of the sub-wallet to create
     * @return [CreatedSubWalletResult] the created sub wallet
     */
    suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult

    /**
     * Deletes an existing sub-wallet.
     * @param walletData the data of the wallet to delete. It includes the walletId and walletKey
     */
    suspend fun deleteSubWallet(walletData: WalletExtendedData)

    /**
     * Retrieves the token of a wallet by its walletId
     * @param id the Id of the wallet
     * @param key the key of the wallet
     * @return [CreateWalletTokenResponse] the token of the wallet
     */
    suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse

    /**
     * Creates a local DID for a Sub Wallet
     * @param didCreateDto the method and option to create the DID
     * @param token the Token of the wallet
     * @return [DidResult] the response including the DID and its Verkey
     */
    suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult

    /**
     * Registers a DID and its verkey on ledger by Base Wallet
     * @param didRegistration the registration data
     * @return [DidRegistrationResult] the response including the status of the request
     */
    suspend fun registerDidOnLedgerUsingBaseWallet(didRegistration: DidRegistration): DidRegistrationResult

    /**
     * Signs a given Json-ld Document.
     * @param signRequest the Json-ld Document to sign. It can be
     * of type Verifiable Credential or Verifiable Presentation
     * @param token the token for managed wallet, null for Base Wallet
     * @return the signed Json-ld Document as String
     */
    suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String?): String

    /**
     * Verifies a given Json-ld Document.
     * @param verifyRequest the Json-ld Document to verify. It can be
     * of type Verifiable Credential or Verifiable Presentation
     * @param token the token for managed wallet, null for Base Wallet
     * @return [VerifyResponse] the verify response
     */
    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String?): VerifyResponse

    /**
     * Resolves a DID and Retrieve its Document
     * @param did the DID
     * @param token the token for managed wallet, null for Base Wallet or external DIDs
     * @return [ResolutionResult] the result of the resolution
     */
    suspend fun resolveDidDoc(did: String, token: String?): ResolutionResult

    /**
     * Updates the service endpoint of the Base Wallet
     * @param serviceEndPoint DidEndpointWithType containing the information of the service endpoint
     */
    suspend fun updateServiceOfBaseWallet(serviceEndPoint: DidEndpointWithType)

    /**
     * Updates the service endpoint of the sub-wallet using endorsement.
     * @param serviceEndPoint DidEndpointWithType containing the information of the service endpoint
     * @param token the token the sub-wallet
     */
    suspend fun updateServiceUsingEndorsement(serviceEndPoint: DidEndpointWithType, token: String)

    /**
     * Subscribes the base wallet for the web socket of Base AcaPy Instance.
     */
    fun subscribeBaseWalletForWebSocket()

    /**
     * Gets the AcaPy Client based on the given walletTokne
     * @param walletToken the token of the wallet. null for Base Wallet
     * @return AriesClient the acies client from the acapy-java-libary
     */
    suspend fun getAcapyClient(walletToken: String?): AriesClient

    suspend fun sendConnectionRequest(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto,
        token: String?
    ): ConnectionRecord

    suspend fun issuanceFlowCredentialSend(
        token: String?,
        vc: VerifiableCredentialIssuanceFlowRequest
    ): V20CredExRecord

    /**
     * Deletes the connection with the given connectionId
     * @param connectionId the id of the connection to be deleted
     * @param token the token of the wallet, null for Base Wallet
     */
    suspend fun deleteConnection(connectionId: String, token: String?)

    /**
     * Accepts a connection request with the given connectionId.
     * @param connectionId the id of the connection
     * @param token the token of the wallet, null for Base Wallet
     * @return [ConnectionRecord] the record of the connection
     */
    suspend fun acceptConnectionRequest(connectionId: String, token: String?): ConnectionRecord

    /**
     * Accepts a credential offer by sending an issue credential request.
     * @param holderDid the DID of the Holder
     * @param credentialExchangeId the id of the credential exchange
     * @param token the token of the wallet, null for Base Wallet
     */
    suspend fun acceptCredentialOfferBySendingRequest(
        holderDid: String,
        credentialExchangeId: String,
        token: String?
    )

    /**
     * Accepts issued credential exchange by storing the issued credential
     * @param credentialId the id of the issued credential
     * @param credentialExchangeId the id of the credential exchange
     * @param token the token of the wallet, null for Base Wallet
     */
    suspend fun acceptCredentialReceivedByStoringIssuedCredential(
        credentialId: String,
        credentialExchangeId: String,
        token: String?
    )

    /**
     * Sends a connection request to the DID.
     * @param didOfTheirWallet the DID of the wallet to send the request to
     * @param usePublicDid a boolean value indicating if the public DID should be used to send the request
     * @param label a label for the connection
     * @return [ConnectionRecord] the connection record of the request
     */
    suspend fun sendConnectionRequest(
        didOfTheirWallet: String,
        usePublicDid: Boolean,
        alias: String?,
        token: String?,
        label: String?
    ): ConnectionRecord

    /**
     * Retrieves all requested connections to the Base Wallet.
     * @return List of [ConnectionRecord] of the requested connections
    */
    suspend fun getRequestedConnectionsToBaseWallet(): List<ConnectionRecord>

    /**
     * Sets the endorser metadata for the connection by Base Wallet.
     * @param connectionId the id of the connection for which to set the endorser metadata
     * @return TransactionJobs the jobs that are created by this transaction. null if fails
     */
    suspend fun setEndorserMetaData(connectionId: String): TransactionJobs?

    /**
     * Sets the author role and info metadata for the connection by sub-wallet.
     * @param connectionId the id of the connection for which to set the author role and info metadata
     * @param endorserDID the DID of the endorser
     * @param token the token of the wallet
     */
    suspend fun setAuthorRoleAndInfoMetaData(connectionId: String, endorserDID: String, token: String)

    /**
     * Sets the given DID as public using the endorser.
     * @param did the DID to set as public
     * @param token the token of the sub-wallet
    */
    suspend fun setDidAsPublicUsingEndorser(did: String, token: String)

    companion object {
        /**
         * Create the AcapyService
         */
        fun create(
            walletAndAcaPyConfig: WalletAndAcaPyConfig,
            utilsService: UtilsService,
            client: HttpClient
        ): IAcaPyService {
            return AcaPyService(walletAndAcaPyConfig, utilsService, client)
        }
    }
}

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
import org.eclipse.tractusx.managedidentitywallets.models.ServicesHttpClientConfig
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
    fun getAuthorityWallet(): WalletExtendedData

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
    fun createWallet(walletCreateDto: WalletCreateDto): WalletDto

    /**
     * TBD the functionalities of this interface
     * Theoretically should only be the referenced deleted
     * @param identifier the BPN or DID of the wallet. Or the wallet id of managed wallets
     * @return true if the wallet is deleted successfully
     * @throws NotFoundException if the wallet does not exist
     */
    fun deleteWallet(identifier: String): Boolean

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
    suspend fun issueAuthorityWalletCredential(
        vcBaseWalletRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto

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
        withRevocationValidation: Boolean,
        asJwt: Boolean
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
     * @throws UnprocessableEntityException if the presentation is not valid or the validation failed
     */
    suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean = false,
        withRevocationValidation: Boolean
    )
}

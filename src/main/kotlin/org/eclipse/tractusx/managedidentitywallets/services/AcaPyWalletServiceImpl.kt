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

import com.google.gson.GsonBuilder
import foundation.identity.jsonld.JsonLDUtils
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallet
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class AcaPyWalletServiceImpl(
    private val acaPyService: IAcaPyService,
    private val walletRepository: WalletRepository,
    private val credentialRepository: CredentialRepository,
    private val utilsService: UtilsService,
    private val revocationService: IRevocationService,
    private val webhookService: IWebhookService,
    private val connectionRepository: ConnectionRepository
) : IWalletService {

    private val baseWalletBpn = acaPyService.getWalletAndAcaPyConfig().baseWalletBpn
    private val baseWalletDid = acaPyService.getWalletAndAcaPyConfig().baseWalletDID

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val gson = GsonBuilder().create()
    }

    override fun getWallet(identifier: String, withCredentials: Boolean): WalletDto {
        log.debug("Get Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            val walletDto = walletRepository.toObject(extractedWallet)
            val credentials: List<VerifiableCredentialDto> =  if (withCredentials) {
                getCredentials(
                    null,
                    holderIdentifier = walletDto.did,
                    null,
                    null
                )
            } else { listOf() }

            WalletDto(
                name = walletDto.name,
                bpn = walletDto.bpn,
                did = walletDto.did,
                verKey = walletDto.verKey,
                createdAt = walletDto.createdAt,
                vcs = credentials,
                revocationListName = walletDto.revocationListName,
                pendingMembershipIssuance = walletDto.pendingMembershipIssuance,
                isSelfManaged = walletDto.isSelfManaged
            )
        }
    }

    override fun getCatenaXWallet(): WalletExtendedData {
        return WalletExtendedData(
            id = null,
            name = "", // currently, not needed anywhere
            bpn = baseWalletBpn,
            did = baseWalletDid,
            pendingMembershipIssuance = false,
            walletId = null,
            walletKey = null,
            walletToken = null,
            revocationListName = null // currently, not needed anywhere
        )
    }

    override fun getAll(): List<WalletDto> {
        log.debug("Get All Wallets")
        return transaction {
            val listOfWallets = walletRepository.getAll()
            listOfWallets.map { walletRepository.toObject(it) }
        }
    }

    override fun getAllBpns(): List<String> {
        log.debug("List BPNs of created wallets")
        return transaction {
            val listOfWallets = walletRepository.getAll()
            listOfWallets.map { walletRepository.toObject(it).bpn }
        }
    }

    override suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
        log.debug("Add a new Wallet with bpn ${walletCreateDto.bpn}")
        // Check if wallet already exists
        transaction { walletRepository.checkWalletAlreadyExists(walletCreateDto.bpn) }
        // Create Sub Wallet in Aca-Py
        val subWalletToCreate = CreateSubWallet(
            keyManagementMode = KeyManagementMode.MANAGED.toString(),
            label = walletCreateDto.name,
            walletWebhookUrls = emptyList(),
            walletDispatchType = WalletDispatchType.BASE.toString(),
            walletKey = utilsService.createRandomString(),
            walletName = walletCreateDto.bpn + "-" + JsonLDUtils.dateToString(Date.from(Instant.now())),
            walletType = WalletType.ASKAR.toString()
        )
        val createdSubWalletDto = acaPyService.createSubWallet(subWalletToCreate)
        // Create local DID in Aca-Py
        val createdDid = acaPyService.createLocalDidForWallet(
            DidCreate(method = DidMethod.SOV.toString(), options = DidCreateOptions(KeyType.ED25519.toString())),
            createdSubWalletDto.token
        )

        // This can also be done using endorsement. but catena-X wallet can register it directly
        acaPyService.registerDidOnLedgerUsingBaseWallet(DidRegistration(
            did = createdDid.result.did,
            alias = walletCreateDto.name,
            verkey = createdDid.result.verkey,
            role = ""
        ))

        // create revocation list
        val revocationListName = revocationService.registerList(createdDid.result.did, issueCredential = false)

        val walletToCreate = WalletExtendedData(
            name = walletCreateDto.name,
            bpn = walletCreateDto.bpn,
            did = "${utilsService.getDidMethodPrefixWithNetworkIdentifier()}${createdDid.result.did}",
            walletId = createdSubWalletDto.walletId,
            walletKey = subWalletToCreate.walletKey,
            walletToken = createdSubWalletDto.token,
            revocationListName = revocationListName,
            pendingMembershipIssuance = true
        )

        val storedWallet = transaction {
            val createdWalletData = walletRepository.addWallet(walletToCreate)
            walletRepository.toObject(createdWalletData)
        }

        acaPyService.sendConnectionRequest(
            didOfTheirWallet = getCatenaXWallet().did,
            usePublicDid = false,
            alias = "endorser",
            token = createdSubWalletDto.token,
            label = walletToCreate.bpn
        )

        return WalletDto(
            storedWallet.name,
            storedWallet.bpn,
            storedWallet.did,
            createdDid.result.verkey,
            storedWallet.createdAt,
            storedWallet.vcs,
            storedWallet.revocationListName,
            storedWallet.pendingMembershipIssuance,
            storedWallet.isSelfManaged
        )
    }

    override suspend fun registerSelfManagedWalletAndBuildConnection(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto
    ): SelfManagedWalletResultDto {

        utilsService.checkIndyDid(selfManagedWalletCreateDto.did)

        transaction {
            walletRepository.checkWalletAlreadyExists(selfManagedWalletCreateDto.did)
        }

        val connectionRecord = acaPyService.sendConnectionRequest(selfManagedWalletCreateDto, null)

        val walletToCreate = WalletExtendedData(
            name = selfManagedWalletCreateDto.name,
            bpn = selfManagedWalletCreateDto.bpn,
            did = selfManagedWalletCreateDto.did,
            walletId = null,
            walletKey = null,
            walletToken = null,
            revocationListName = null,
            pendingMembershipIssuance = true
        )

        return transaction {
            val createdWalletData = walletRepository.addWallet(walletToCreate)
            connectionRepository.add(
                idOfConnection = connectionRecord.connectionId,
                connectionOwnerDid = getCatenaXWallet().did,
                connectionTargetDid = selfManagedWalletCreateDto.did,
                rfc23State =  connectionRecord.rfc23State
            )
            if (!selfManagedWalletCreateDto.webhookUrl.isNullOrBlank()) {
                webhookService.addWebhook(
                    // The ConnectionRecord has no threadId. Therefore, the requestId as an Identifier.
                    threadId = connectionRecord.requestId,
                    url = selfManagedWalletCreateDto.webhookUrl,
                    state = connectionRecord.rfc23State
                )
            }
            SelfManagedWalletResultDto(
                createdWalletData.name,
                createdWalletData.bpn,
                createdWalletData.did,
                createdWalletData.createdAt
            )
        }
    }

    override suspend fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        val walletData = getWalletExtendedInformation(identifier)
        val isSelfManagedWallet = walletData.walletId.isNullOrBlank()
        transaction {
            val connections = connectionRepository.getConnections(null, walletData.did)
            connections.forEach {
                val connectionOwnerWallet = getWalletExtendedInformation(it.myDid)
                runBlocking {
                    acaPyService.deleteConnection(
                        connectionId = it.connectionId,
                        token = connectionOwnerWallet.walletToken
                    )
                }
            }
            credentialRepository.deleteCredentialsOfWallet(walletId = walletData.id!!)
            connectionRepository.deleteConnections(walletData.did)
            walletRepository.deleteWallet(identifier)
        }
        if (!isSelfManagedWallet && !isCatenaXWallet(walletData.bpn)) {
            acaPyService.deleteSubWallet(walletData)
        }
        return true
    }

    override fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean {
        log.debug("Store Credential in Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            val credentialAsJson =
                Json.encodeToString(IssuedVerifiableCredentialRequestDto.serializer(), issuedCredential)
            issuedCredential.type.map { }
            val listOfTypes = issuedCredential.type.sorted().toMutableList()
            if (listOfTypes.size > 1) {
                listOfTypes.remove("VerifiableCredential")
            }
            if (extractedWallet.did != issuedCredential.credentialSubject["id"]) {
                throw ForbiddenException(
                    """
                        The target wallet $identifier is not the holder ${issuedCredential.credentialSubject["id"]}
                    """.trimIndent()
                )
            }
            credentialRepository.storeCredential(
                issuedCredential,
                credentialAsJson,
                listOfTypes[0],
                extractedWallet
            )
            true
        }
    }

    override suspend fun deleteCredential(credentialId: String): Boolean {
        return credentialRepository.deleteCredentialByCredentialId(credentialId)
    }

    override suspend fun issueCatenaXCredential(
        vcCatenaXRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto {
        val verifiableCredentialRequestDto = VerifiableCredentialRequestDto(
            id = vcCatenaXRequest.id,
            context = vcCatenaXRequest.context,
            type = vcCatenaXRequest.type,
            issuerIdentifier = baseWalletBpn,
            issuanceDate = vcCatenaXRequest.issuanceDate,
            expirationDate = vcCatenaXRequest.expirationDate,
            credentialSubject = vcCatenaXRequest.credentialSubject,
            holderIdentifier = vcCatenaXRequest.holderIdentifier,
            isRevocable = vcCatenaXRequest.isRevocable
        )
        return issueCredential(verifiableCredentialRequestDto)
    }

    override suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        log.debug("Issue Credential $vcRequest")
        val issuerWalletData = getWalletExtendedInformation(vcRequest.issuerIdentifier)
        val issuerDid = issuerWalletData.did
        val credentialSubject = vcRequest.credentialSubject.toMutableMap()
        // If the ID of Credential Subject exist then ignore the given holderIdentifier.
        if (!credentialSubject.containsKey("id") && !vcRequest.holderIdentifier.isNullOrBlank()) {
            credentialSubject["id"] = try {
                val holderWalletData = getWalletExtendedInformation(vcRequest.holderIdentifier)
                holderWalletData.did
            } catch (e: NotFoundException) {
                vcRequest.holderIdentifier
            }
        }

        val context = vcRequest.context.toMutableList()
        var credentialStatus: CredentialStatus? = null
        if (vcRequest.isRevocable) {
            credentialStatus = revocationService.addStatusEntry(utilsService.getIdentifierOfDid(issuerDid))
            context.add(JsonLdContexts.JSONLD_CONTEXT_W3C_STATUS_LIST_2021_V1)
        }
        val verificationMethod = getVerificationMethod(issuerDid, 0)
        val convertedDatetime: Date = Date.from(Instant.now())
        val verifiableCredentialToSign = VerifiableCredentialDto(
            id = vcRequest.id,
            context = context,
            type = vcRequest.type,
            issuer = issuerDid,
            issuanceDate = vcRequest.issuanceDate ?: JsonLDUtils.dateToString(convertedDatetime),
            credentialSubject = credentialSubject,
            credentialStatus = credentialStatus,
            expirationDate = vcRequest.expirationDate
        )

        val signedVcResult: SignCredentialResponse =
            signVerifiableCredential(verifiableCredentialToSign, verificationMethod, issuerWalletData)
        if (signedVcResult.signedDoc != null) {
            return signedVcResult.signedDoc
        }
        throw BadRequestException(signedVcResult.error)
    }

    override suspend fun resolveDocument(identifier: String): DidDocumentDto {
        log.debug("Resolve DID Document $identifier")
        var token: String? = null
        val modifiedDid: String
        if (utilsService.isDID(identifier)) {
            utilsService.checkIndyDid(identifier)
            modifiedDid = utilsService.replaceNetworkIdentifierWithSov(identifier)
        } else {
            val walletData = getWalletExtendedInformation(identifier)
            token = walletData.walletToken
            modifiedDid = utilsService.replaceNetworkIdentifierWithSov(walletData.did)
        }
        val didDocResult = acaPyService.resolveDidDoc(modifiedDid, token)
        val resolutionResultAsJson = Json.encodeToString(ResolutionResult.serializer(), didDocResult)
        val res: ResolutionResult =
            Json.decodeFromString(utilsService.replaceSovWithNetworkIdentifier(resolutionResultAsJson))
        return res.didDoc
    }

    private suspend fun getVerificationMethod(identifier: String, atIndex: Int): DidVerificationMethodDto {
        log.debug("Get Verification Method for $identifier")
        val didDocumentDto = resolveDocument(identifier)
        if (didDocumentDto.verificationMethods.isNullOrEmpty() || didDocumentDto.verificationMethods.size <= atIndex) {
            throw BadRequestException("Error: no verification methods")
        }
        return didDocumentDto.verificationMethods[atIndex]
    }

    private fun getVerificationKey(verificationMethod: DidVerificationMethodDto, type: String): String {
        return when (type) {
            VerificationKeyType.PUBLIC_KEY_BASE58.toString() -> verificationMethod.publicKeyBase58
                ?: throw BadRequestException("Verification Key with publicKeyBase58 does not exist")
            else -> {
                throw BadRequestException("Not supported public key type")
            }
        }
    }

    override suspend fun issuePresentation(
        vpRequest: VerifiablePresentationRequestDto,
        withCredentialsValidation: Boolean,
        withCredentialsDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifiablePresentationDto {
        log.debug("Issue Presentation $vpRequest")
        val holderWalletData = getWalletExtendedInformation(vpRequest.holderIdentifier)
        val holderDid = holderWalletData.did
        val token = holderWalletData.walletToken
        val verificationMethod = getVerificationMethod(vpRequest.holderIdentifier, 0)
        if (withCredentialsValidation) {
            vpRequest.verifiableCredentials.forEach {
                validateVerifiableCredential(it, withCredentialsDateValidation, withRevocationValidation, token)
            }
        }
        val signRequest: SignRequest<VerifiablePresentationDto> = SignRequest(
            doc = SignDoc(
                credential = VerifiablePresentationDto(
                    id = "urn:uuid:${UUID.randomUUID()}",
                    context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
                    type = listOf("VerifiablePresentation"),
                    holder = holderDid,
                    verifiableCredential = vpRequest.verifiableCredentials
                ),
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = utilsService.replaceSovWithNetworkIdentifier(verificationMethod.id)
                )
            ),
            verkey = getVerificationKey(verificationMethod, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
        )
        val signedVpAsJsonString = acaPyService.signJsonLd(signRequest, token)
        val signedVpResult: SignPresentationResponse = Json.decodeFromString(signedVpAsJsonString)
        if (signedVpResult.signedDoc != null) {
            return signedVpResult.signedDoc
        }
        throw BadRequestException(signedVpResult.error)
    }

    override suspend fun addService(identifier: String, serviceDto: DidServiceDto) {
        log.debug("Add Service Endpoint for $identifier")
        utilsService.checkSupportedId(serviceDto.id)
        val walletData = getWalletExtendedInformation(identifier)
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            didDoc.services.map {
                if (it.id.split("#")[1] == serviceDto.id) {
                    throw ConflictException("Service end point already exists")
                }
            }
        }

        if (isCatenaXWallet(walletData.bpn)) {
            acaPyService.updateServiceOfBaseWallet(
                DidEndpointWithType(
                    didIdentifier = utilsService.getIdentifierOfDid(walletData.did),
                    endpoint = serviceDto.serviceEndpoint,
                    endpointType = utilsService.mapServiceTypeToEnum(serviceDto.type)
                )
            )
        } else {
            acaPyService.updateServiceUsingEndorsement(
                DidEndpointWithType(
                    didIdentifier = utilsService.getIdentifierOfDid(walletData.did),
                    endpoint = serviceDto.serviceEndpoint,
                    endpointType = utilsService.mapServiceTypeToEnum(serviceDto.type)
                ),
                walletData.walletToken!!
            )
        }
    }

    override suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ) {
        log.debug("Update Service Endpoint for $identifier")
        utilsService.checkSupportedId(id)
        val walletData = getWalletExtendedInformation(identifier)
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            var found = false
            didDoc.services.filter {
                it.id.split("#")[1] == id
            }.map {
                if (isCatenaXWallet(walletData.bpn)) {
                    acaPyService.updateServiceOfBaseWallet(
                        DidEndpointWithType(
                            didIdentifier = utilsService.getIdentifierOfDid(walletData.did),
                            endpoint = serviceUpdateRequestDto.serviceEndpoint,
                            endpointType = utilsService.mapServiceTypeToEnum(serviceUpdateRequestDto.type)
                        )
                    )
                } else {
                    acaPyService.updateServiceUsingEndorsement(
                        DidEndpointWithType(
                            didIdentifier = utilsService.getIdentifierOfDid(walletData.did),
                            endpoint = serviceUpdateRequestDto.serviceEndpoint,
                            endpointType = utilsService.mapServiceTypeToEnum(serviceUpdateRequestDto.type)
                        ),
                        walletData.walletToken!!
                    )
                }
                found = true
            }
            if (!found) {
                throw NotFoundException("Target Service Endpoint not Found")
            }
            return
        }
        throw BadRequestException("Update Service failed: DID Document has no services")
    }

    override suspend fun deleteService(identifier: String, id: String): DidDocumentDto {
        throw NotImplementedException("Delete Service Endpoint is not supported!")
    }

    override fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto> {
        val issuerDid = if (!issuerIdentifier.isNullOrEmpty()) {
            getWallet(issuerIdentifier).did
        } else {
            null
        }
        val holderDid = if (!holderIdentifier.isNullOrEmpty()) {
            getWallet(holderIdentifier).did
        } else {
            null
        }
        return credentialRepository.getCredentials(issuerDid, holderDid, type, credentialId)
    }

    private fun isCatenaXWallet(bpn: String): Boolean = bpn == baseWalletBpn

    override suspend fun verifyVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        withDateValidation: Boolean,
        withRevocationValidation: Boolean
    ): VerifyResponse {
        validateVerifiablePresentation(vpDto, null)

        val listOfVerifiableCredentials = vpDto.verifiableCredential
        if (!listOfVerifiableCredentials.isNullOrEmpty()) {
            listOfVerifiableCredentials.forEach {
                validateVerifiableCredential(
                    it,
                    withDateValidation,
                    withRevocationValidation,
                    null
                )
            }
        }
        return VerifyResponse(error = null, valid = true, vp = vpDto)
    }

    override fun getDidFromBpn(bpn: String): String = getWallet(bpn, false).did

    override fun getBpnFromDid(did: String): String = getWallet(did, false).bpn

    override fun getBpnFromIdentifier(identifier: String): String {
        return if (utilsService.isDID(identifier)) {
            getWallet(identifier).bpn
        } else {
            identifier
        }
    }

    private suspend fun validateVerifiableCredential(
        vc: VerifiableCredentialDto,
        withDateValidation: Boolean,
        withRevocationCheck: Boolean,
        walletToken: String?
    ) {
        if (withDateValidation) {
            val currentDatetime: Date = Date.from(Instant.now())
            if (currentDatetime.before(JsonLDUtils.stringToDate(vc.issuanceDate))) {
                throw UnprocessableEntityException("Invalid issuance date ${vc.issuanceDate} " +
                        "in verifiable credential ${vc.id}")
            }
            if (!vc.expirationDate.isNullOrEmpty()
                && currentDatetime.after(JsonLDUtils.stringToDate(vc.expirationDate))
            ) {
                throw UnprocessableEntityException("Verifiable credential ${vc.id} expired ${vc.expirationDate}")
            }
        }
        if (vc.proof == null) {
            throw UnprocessableEntityException("Cannot verify verifiable credential ${vc.id} due to missing proof")
        }
        if (withRevocationCheck) {
            validateRevocation(vc, walletToken)
        }
        val verifyReq = VerifyRequest(
            signedDoc = vc,
            verkey = getVerKeyOfVerificationMethodId(vc.issuer, vc.proof.verificationMethod)
        )
        var isValid = true
        var message = ""
        try {
            val response = acaPyService.verifyJsonLd(verifyReq, walletToken)
            if (!response.valid) {
                isValid = false
                message = if (response.error.isNullOrBlank()) message else " Error message ${response.error}"
            }
        } catch (e: Exception) {
            throw UnprocessableEntityException("External validation of the verifiable credential ${vc.id}" +
                    " failed: ${e.message}")
        }
        if (!isValid) {
            throw UnprocessableEntityException("Verifiable credential with id ${vc.id} is not valid.$message")
        }
    }

    private suspend fun validateVerifiablePresentation(
        vpDto: VerifiablePresentationDto,
        walletToken: String?
    ) {
        if (vpDto.proof == null) {
            throw UnprocessableEntityException("Cannot verify verifiable presentation due to missing proof")
        }
        val didOfVpSigner = if (vpDto.holder.isNullOrEmpty()) {
            vpDto.proof.verificationMethod.split("#").first()
        } else {
            vpDto.holder
        }
        val verifyVPReq = VerifyRequest(
            signedDoc = vpDto,
            verkey = getVerKeyOfVerificationMethodId(didOfVpSigner, vpDto.proof.verificationMethod)
        )
        var isValid = true
        var message = ""
        try {
            val response = acaPyService.verifyJsonLd(verifyVPReq, walletToken)
            if (!response.valid) {
                isValid = false
                message = if (response.error.isNullOrBlank()) message else " Error message ${response.error}"
            }
        } catch (e: Exception) {
            throw UnprocessableEntityException("External validation of " +
                    "the verifiable presentation failed: ${e.message}")
        }
        if (!isValid) {
            throw UnprocessableEntityException("Verifiable presentation is not valid.$message")
        }
    }

    private suspend fun getVerKeyOfVerificationMethodId(did: String, verificationMethodId: String): String {
        val didDocumentDto = resolveDocument(did)
        if (didDocumentDto.verificationMethods.isNullOrEmpty()) {
            throw UnprocessableEntityException("The DID Doc has no verification methods")
        }
        val verificationMethod = didDocumentDto.verificationMethods.find {
                method -> method.id == verificationMethodId
        } ?: throw UnprocessableEntityException("Verification method with given Id " +
                "$verificationMethodId does not exist")
        return getVerificationKey(verificationMethod, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
    }

    private fun getWalletExtendedInformation(identifier: String): WalletExtendedData {
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            walletRepository.toWalletCompleteDataObject(extractedWallet)
        }
    }

    private suspend fun validateRevocation(vc: VerifiableCredentialDto, walletToken: String?) {
        // Credential is not revocable
        if (vc.credentialStatus == null) {
            return
        }
        verifyPropertiesOfCredentialStatus(vc.id, vc.credentialStatus)

        val statusListVerifiableCredential = revocationService.getStatusListCredentialOfUrl(vc.credentialStatus.listUrl)

        if (statusListVerifiableCredential.issuer != vc.issuer) {
            throw UnprocessableEntityException("Cannot verify revocation: " +
                    "The issuer of the given credential ${vc.id} is not the issuer of the StatusListCredential")
        }

        val listCredentialSubjectAsMap  = statusListVerifiableCredential.credentialSubject

        if (listCredentialSubjectAsMap.containsKey(ListCredentialSubject.CREDENTIAL_TYPE_KEY) &&
            listCredentialSubjectAsMap[ListCredentialSubject.CREDENTIAL_TYPE_KEY] != ListCredentialSubject.CREDENTIAL_TYPE) {
            throw UnprocessableEntityException("Cannot verify revocation status of credential ${vc.id} " +
                    "due to wrong type of extracted StatusList Credential")
        }
        if (listCredentialSubjectAsMap.containsKey(ListCredentialSubject.STATUS_PURPOSE_KEY) &&
            listCredentialSubjectAsMap[ListCredentialSubject.STATUS_PURPOSE_KEY] != ListCredentialSubject.STATUS_PURPOSE) {
            throw UnprocessableEntityException("Cannot verify revocation status of credential ${vc.id} " +
                    "due to wrong statusPurpose of extracted StatusList Credential")
        }
        if (listCredentialSubjectAsMap.containsKey(ListCredentialSubject.ENCODED_LIST_KEY) &&
            listCredentialSubjectAsMap[ListCredentialSubject.ENCODED_LIST_KEY].toString().isBlank()) {
            throw UnprocessableEntityException("Cannot verify revocation status of credential ${vc.id} " +
                    "due to empty or null encodedList of extracted StatusList Credential")
        }

        validateVerifiableCredential(
            vc = statusListVerifiableCredential,
            withDateValidation = true,
            withRevocationCheck = false,
            walletToken = walletToken
        )

        val bitSet = utilsService.decodeBitset(listCredentialSubjectAsMap["encodedList"] as String)
        if (bitSet.get(Integer.valueOf(vc.credentialStatus.index))) {
            throw UnprocessableEntityException("The credential ${vc.id} has been revoked!")
        }
    }

    override suspend fun issueStatusListCredential(
        profileName: String,
        listCredentialRequestData: ListCredentialRequestData
    ): VerifiableCredentialDto {
        val issuerDid = "${utilsService.getDidMethodPrefixWithNetworkIdentifier()}$profileName"
        val verifiableCredentialRequestDto = VerifiableCredentialRequestDto(
            id = listCredentialRequestData.listId,
            context = listOf(
                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                JsonLdContexts.JSONLD_CONTEXT_W3C_STATUS_LIST_2021_V1
            ),
            type = listOf("VerifiableCredential", "StatusList2021Credential"),
            issuerIdentifier = issuerDid,
            credentialSubject = mapOf(
                "id" to listCredentialRequestData.subject.credentialId,
                "type" to "StatusList2021",
                "statusPurpose" to "revocation",
                "encodedList" to listCredentialRequestData.subject.encodedList
            ),
            issuanceDate = JsonLDUtils.dateToString(Date.from(Instant.now())),
            isRevocable = false
        )
        return issueCredential(verifiableCredentialRequestDto)
    }

    private suspend fun signVerifiableCredential(
        verifiableCredentialToSign: VerifiableCredentialDto,
        verificationMethod: DidVerificationMethodDto,
        issuerWalletData: WalletExtendedData
    ): SignCredentialResponse {
        val signRequest: SignRequest<VerifiableCredentialDto> = SignRequest(
            SignDoc(
                credential = verifiableCredentialToSign,
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = utilsService.replaceSovWithNetworkIdentifier(verificationMethod.id)
                )
            ),
            verkey = getVerificationKey(verificationMethod, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
        )
        val signedVcResultAsJsonString = acaPyService.signJsonLd(signRequest, issuerWalletData.walletToken)
        return Json.decodeFromString(signedVcResultAsJsonString)
    }

    override suspend fun revokeVerifiableCredential(vc: VerifiableCredentialDto) {
        val issuerDid = vc.issuer
        val walletOfIssuer = getWalletExtendedInformation(issuerDid)

        if (vc.credentialStatus == null) {
            throw UnprocessableEntityException("The given verifiable credential is not revocable!")
        }
        verifyPropertiesOfCredentialStatus(vc.id, vc.credentialStatus)

        validateVerifiableCredential(vc,
            withDateValidation = false, withRevocationCheck = false, walletOfIssuer.walletToken)

        val profileName = utilsService.getIdentifierOfDid(walletOfIssuer.did)
        revocationService.revoke(
            profileName = profileName,
            indexOfCredential = vc.credentialStatus.index.toLong()
        )
        revocationService.issueStatusListCredentials(profileName, true)
    }

    override fun updateConnectionState(connectionId: String, rfc23State: String) {
        connectionRepository.updateConnectionState(connectionId, rfc23State)
    }

    override fun setPartnerMembershipIssued(walletDto: WalletDto) {
        return walletRepository.updatePending(
            did = walletDto.did,
            isPending = false
        )
    }

    override fun getConnection(connectionId: String): ConnectionDto? {
        return transaction {
            val connection = connectionRepository.getOrNull(connectionId)
            if (connection != null) {
                 connectionRepository.toObject(connection)
            } else {
                null
            }
        }
    }

    override fun getConnectionWithCatenaX(theirDid: String): ConnectionDto? {
        return getConnections(getCatenaXWallet().did, theirDid).firstOrNull()
    }

    override suspend fun triggerCredentialIssuanceFlow(
        vc: VerifiableCredentialIssuanceFlowRequest
    ): CredentialOfferResponse {
        val  issuerWallet = getWalletExtendedInformation(vc.issuerIdentifier)
        // Check the Holder
        val credentialSubject = vc.credentialSubject.toMutableMap()
        val holderWallet: Wallet = if (credentialSubject.containsKey("id")) {
            val wallet = walletRepository.getWallet(credentialSubject["id"] as String)
            credentialSubject["id"] = utilsService.replaceNetworkIdentifierWithSov(wallet.did)
            wallet
        } else if (!vc.holderIdentifier.isNullOrBlank()) {
            val wallet = walletRepository.getWallet(vc.holderIdentifier)
            credentialSubject["id"] = utilsService.replaceNetworkIdentifierWithSov(wallet.did)
            wallet
        } else {
            throw UnprocessableEntityException("The credential subject id aka. Holder is not defined")
        }

        // Check connections
        val connection = connectionRepository.getConnections(issuerWallet.did, holderWallet.did).firstOrNull()
        if (connection == null || connection.state != Rfc23State.COMPLETED.toString()) {
            throw InternalServerErrorException("Invalid connection between " +
                    "${issuerWallet.did} and ${holderWallet.did}")
        }

        val vcContext: List<String> = if (vc.isRevocable) {
            val mutableContexts = vc.context.toMutableList()
            mutableContexts.add(JsonLdContexts.JSONLD_CONTEXT_W3C_STATUS_LIST_2021_V1)
            mutableContexts
        } else { vc.context }
        val vcAcapyRequest = VerifiableCredentialIssuanceFlowRequest(
            id = vc.id ,
            context = vcContext,
            type = vc.type,
            issuerIdentifier = utilsService.replaceNetworkIdentifierWithSov(issuerWallet.did),
            issuanceDate = vc.issuanceDate,
            expirationDate = vc.expirationDate,
            credentialSubject = credentialSubject,
            credentialStatus = null,  // currently, not supported in issuance flow
            webhookUrl = vc.webhookUrl,
            connectionId = connection.connectionId
        )

        val v20CredExRecord = acaPyService.issuanceFlowCredentialSend(
            token = issuerWallet.walletToken, // null for catenaX wallet
            vc = vcAcapyRequest
        )

        transaction {
            if (!vc.webhookUrl.isNullOrBlank()) {
                webhookService.addWebhook(
                    v20CredExRecord.threadId,
                    vc.webhookUrl,
                    CredentialExchangeState.OFFER_SENT.name
                )
            }
        }
        return CredentialOfferResponse(
            credentialOffer = String(
                Base64.getDecoder().decode(v20CredExRecord.credOffer.offersAttach[0].data.base64),
                Charsets.UTF_8
            ),
            threadId = v20CredExRecord.threadId
        )
    }

    override suspend fun acceptConnectionRequest(identifier: String, connectionRecord: ConnectionRecord) {
        transaction {
            runBlocking {
                val targetWallet = getWalletExtendedInformation(identifier)
                val updateConnectionRecord = acaPyService.acceptConnectionRequest(
                    connectionRecord.connectionId,
                    targetWallet.walletToken // null for base wallet
                )
                if (updateConnectionRecord.rfc23State != "response-sent") {
                    log.error("Expected rdc23State ${updateConnectionRecord.rfc23State} of wallet ${targetWallet.bpn} " +
                            "is not equal to response-sent")
                }
            }
        }
    }

    override suspend fun acceptReceivedOfferVc(identifier: String, credExRecord: V20CredExRecord) {
        val offerReceiverWallet = getWalletExtendedInformation(identifier)
        acaPyService.acceptCredentialOfferBySendingRequest(
            offerReceiverWallet.did,
            credExRecord.credentialExchangeId,
            offerReceiverWallet.walletToken
        )
    }

    override suspend fun acceptAndStoreReceivedIssuedVc(identifier: String, credExRecord: V20CredExRecord) {
        transaction {
            runBlocking {
                val extractedWallet = walletRepository.getWallet(identifier)
                val credentialId = credExRecord.resolveLDCredential().credential.id ?: credExRecord.credentialExchangeId

                acaPyService.acceptCredentialReceivedByStoringIssuedCredential(
                    credentialId,
                    credExRecord.credentialExchangeId,
                    extractedWallet.walletToken
                )

                val verifiableCredential = credExRecord.resolveLDCredential().credential
                val types = verifiableCredential.type
                    .filter { it != JsonLdTypes.CREDENTIAL_TYPE }.joinToString(",")

                val idOfSubject: String = if (verifiableCredential.credentialSubject.has("id")) {
                    verifiableCredential.credentialSubject.get("id").asString
                } else {
                    extractedWallet.did
                }

                credentialRepository.storeCredential(
                    issuedCredentialId = credentialId,
                    issuerOfCredential = verifiableCredential.issuer!!,
                    holderOfCredential = idOfSubject,
                    credentialAsJson = gson.toJson(credExRecord.resolveLDCredential().credential),
                    typesAsString = types,
                    holderWallet = extractedWallet
                )
            }
        }
    }

    override fun addConnection(
        connectionId: String,
        connectionTargetDid: String,
        connectionOwnerDid: String,
        connectionState: String
    ) {
        connectionRepository.add(
            idOfConnection = connectionId,
            connectionOwnerDid = connectionOwnerDid,
            connectionTargetDid = connectionTargetDid,
            rfc23State = connectionState
        )
    }

    override suspend fun initCatenaXWalletAndSubscribeForAriesWS(
        bpn: String,
        did: String,
        verkey: String,
        name: String
    ) {
        val walletExists = walletRepository.isWalletExists(bpn)
        if (!walletExists) {
            transaction {
                // create wallet in DB
                val walletData = WalletExtendedData(
                    null,
                    name,
                    bpn,
                    did,
                    null,
                    null,
                    null,
                    null,
                    false

                )
                walletRepository.addWallet(walletData)
            }
        }
        acaPyService.subscribeBaseWalletForWebSocket()

        if (!walletExists) {
            val revocationListName = revocationService.registerList(
                utilsService.getIdentifierOfDid(did),
                issueCredential = false
            )
            transaction {
                walletRepository.addRevocationList(
                    did,
                    revocationListName
                )
            }
            // TODO It can be used after updating ktor and have server-ready event
            // Check the TO-DO in Application.kt
            // revocationService.issueStatusListCredentials(
               // profileName = utilsService.getIdentifierOfDid(did),
               //  force = true
            // )
        }

    }

    private fun getConnections(myDid: String?, theirDid: String?): List<ConnectionDto> {
        return connectionRepository.getConnections(myDid, theirDid)
    }

    private fun verifyPropertiesOfCredentialStatus(
        credentialId: String? = "",
        credentialStatus: CredentialStatus
    ) {
        if (credentialStatus.credentialType != CredentialStatus.CREDENTIAL_TYPE) {
            throw UnprocessableEntityException("Credential with Id $credentialId has invalid credential status 'Type'")
        }
        if (credentialStatus.statusPurpose != CredentialStatus.STATUS_PURPOSE) {
            throw UnprocessableEntityException("Credential with Id $credentialId has invalid 'statusPurpose'")
        }
        if (credentialStatus.index.isBlank() || credentialStatus.index.toLong() < 0) {
            throw UnprocessableEntityException("Credential with Id $credentialId has invalid 'statusListIndex'")
        }
        if (credentialStatus.listUrl.isBlank()) {
            throw UnprocessableEntityException("Credential with Id $credentialId has invalid 'statusListCredential'")
        }
    }


    override suspend fun setCommunicationEndpointUsingEndorsement(
        walletId: String
    ) {
        val walletExtendedData = getWalletExtendedInformation(walletId)
        acaPyService.setDidAsPublicUsingEndorser(
            did = utilsService.getIdentifierOfDid(walletExtendedData.did),
            token = walletExtendedData.walletToken!!
        )
    }

    override suspend fun setAuthorMetaData(
        walletId: String,
        connectionId: String
    ) {
        val extendedData = getWalletExtendedInformation(walletId)
        acaPyService.setAuthorRoleAndInfoMetaData(
            connectionId,
            utilsService.getIdentifierOfDid(getCatenaXWallet().did),
            extendedData.walletToken!!
        )
    }

    override suspend fun sendInvitation(identifier: String, invitationRequestDto: InvitationRequestDto) {
        val extendedData = getWalletExtendedInformation(identifier)
        val connection = acaPyService.sendConnectionRequest(
            didOfTheirWallet = invitationRequestDto.theirPublicDid,
            usePublicDid = true,
            alias = invitationRequestDto.alias,
            token = extendedData.walletToken,
            label = invitationRequestDto.myLabel,
        )
        transaction {
            addConnection(
                connectionId = connection.connectionId,
                connectionTargetDid = connection.theirPublicDid ?: connection.theirDid,
                connectionOwnerDid = extendedData.did,
                connectionState = connection.rfc23State
            )
        }
    }

    override suspend fun setEndorserMetaDataForAcapyConnection(connectionId: String) {
        acaPyService.setEndorserMetaData(connectionId)
    }

}

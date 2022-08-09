/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Instant
import java.util.*

class AcaPyWalletServiceImpl(
    private val acaPyService: IAcaPyService,
    private val walletRepository: WalletRepository,
    private val credentialRepository: CredentialRepository
) : IWalletService {

    private val networkIdentifier = acaPyService.getWalletAndAcaPyConfig().networkIdentifier
    private val baseWalletBpn = acaPyService.getWalletAndAcaPyConfig().baseWalletBpn
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val arrayOfSupportedIds = listOf("did-communication", "linked_domains", "profile")

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    override fun getWallet(identifier: String, withCredentials: Boolean): WalletDto {
        log.debug("Get Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            val walletDto = walletRepository.toObject(extractedWallet)
            if (withCredentials) {
                val credentials = getCredentials(
                    null,
                    holderIdentifier = walletDto.did,
                    null,
                    null
                )
                WalletDto(
                    walletDto.name,
                    walletDto.bpn,
                    walletDto.did,
                    walletDto.verKey,
                    walletDto.createdAt,
                    credentials
                )
            } else {
                walletDto
            }
        }
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

    override suspend fun registerBaseWallet(verKey: String): Boolean {
        log.debug("Register base wallet with bpn $baseWalletBpn and key $verKey")
        val catenaXWallet = getWalletExtendedInformation(baseWalletBpn)
        val shortDid = catenaXWallet.did.substring(("did:indy:$networkIdentifier:").length)

        // Register DID with public DID on ledger
        acaPyService.assignDidToPublic(
            shortDid,
            catenaXWallet.walletToken
        )

        return true
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
            walletKey = createRandomString(),
            walletName = walletCreateDto.bpn + "-" + JsonLDUtils.dateToString(Date.from(Instant.now())),
            walletType = WalletType.ASKAR.toString()
        )
        val createdSubWalletDto = acaPyService.createSubWallet(subWalletToCreate)
        // Create local DID in Aca-Py
        val createdDid = acaPyService.createLocalDidForWallet(
            DidCreate(method = DidMethod.SOV.toString(), options = DidCreateOptions(KeyType.ED25519.toString())),
            createdSubWalletDto.token
        )

        // For Catena-X Wallet 
        //   1. The DID will be registered externally with endorser role.
        //   2. The Assign to public will be triggered manually
        if (!isCatenaXWallet(walletCreateDto.bpn)) {
            registerSubWalletUsingCatenaXWallet(walletCreateDto, createdDid)
        }
        val walletToCreate = WalletExtendedData(
            name = walletCreateDto.name,
            bpn = walletCreateDto.bpn,
            did = "did:indy:${networkIdentifier}:${createdDid.result.did}",
            walletId = createdSubWalletDto.walletId,
            walletKey = subWalletToCreate.walletKey,
            walletToken = createdSubWalletDto.token
        )
        val storedWallet = transaction {
            val createdWalletData = walletRepository.addWallet(walletToCreate)
            walletRepository.toObject(createdWalletData)
        }
        return WalletDto(
            storedWallet.name,
            storedWallet.bpn,
            storedWallet.did,
            createdDid.result.verkey,
            storedWallet.createdAt,
            storedWallet.vcs
        )
    }

    private suspend fun registerSubWalletUsingCatenaXWallet(walletCreateDto: WalletCreateDto, createdDid: DidResult) {
        val catenaXWallet = getWalletExtendedInformation(baseWalletBpn)
        // Register DID on ledger
        acaPyService.registerDidOnLedger(
            DidRegistration(
                alias = walletCreateDto.name,
                did = createdDid.result.did,
                verkey = createdDid.result.verkey,
                role = "NONE"
            ),
            catenaXWallet.walletToken
        )
    }

    override suspend fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        val walletData = getWalletExtendedInformation(identifier)
        transaction {
            credentialRepository.deleteCredentialsOfWallet(walletId = walletData.id!!)
            walletRepository.deleteWallet(identifier)
        }
        acaPyService.deleteSubWallet(walletData)
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

    override suspend fun issueCatenaXCredential(
        vcCatenaXRequest: VerifiableCredentialRequestWithoutIssuerDto
    ): VerifiableCredentialDto {
        log.debug("Issue CatenaX Credential $vcCatenaXRequest")
        val verifiableCredentialRequestDto = VerifiableCredentialRequestDto(
            id = vcCatenaXRequest.id,
            context = vcCatenaXRequest.context,
            type = vcCatenaXRequest.type,
            issuerIdentifier = baseWalletBpn,
            issuanceDate = vcCatenaXRequest.issuanceDate,
            expirationDate = vcCatenaXRequest.expirationDate,
            credentialSubject = vcCatenaXRequest.credentialSubject,
            holderIdentifier = vcCatenaXRequest.holderIdentifier,
        )
        return issueCredential(verifiableCredentialRequestDto)
    }

    override suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        log.debug("Issue Credential $vcRequest")
        val issuerWalletData = getWalletExtendedInformation(vcRequest.issuerIdentifier)
        val holderWalletData = getWalletExtendedInformation(vcRequest.holderIdentifier)

        val issuerDid = issuerWalletData.did
        val holderDid = holderWalletData.did
        val credentialSubject = vcRequest.credentialSubject.toMutableMap()
        if (credentialSubject["id"] != null || credentialSubject["id"] != holderDid) {
            credentialSubject["id"] = holderDid
        }
        val verificationMethod = getVerificationMethod(issuerDid, 0)
        val convertedDatetime: Date = Date.from(Instant.now())
        val issuanceDate = vcRequest.issuanceDate ?: JsonLDUtils.dateToString(convertedDatetime)
        val signRequest: SignRequest<VerifiableCredentialDto> = SignRequest(
            SignDoc(
                credential = VerifiableCredentialDto(
                    id = vcRequest.id,
                    context = vcRequest.context,
                    type = vcRequest.type,
                    issuer = issuerDid,
                    issuanceDate = issuanceDate,
                    credentialSubject = credentialSubject,
                    expirationDate = vcRequest.expirationDate
                ),
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = replaceSovWithNetworkIdentifier(verificationMethod.id)
                )
            ),
            verkey = getVerificationKey(verificationMethod, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
        )
        val signedVcResultAsJsonString = acaPyService.signJsonLd(signRequest, issuerWalletData.walletToken)
        val signedVcResult: SignCredentialResponse = Json.decodeFromString(signedVcResultAsJsonString)
        if (signedVcResult.signedDoc != null) {
            return signedVcResult.signedDoc
        }
        throw BadRequestException(signedVcResult.error)
    }

    override suspend fun resolveDocument(identifier: String): DidDocumentDto {
        log.debug("Resolve DID Document $identifier")
        val token: String
        val modifiedDid: String
        if (isDID(identifier)) {
            val catenaXWallet = getWalletExtendedInformation(baseWalletBpn)
            token = catenaXWallet.walletToken
            modifiedDid = replaceNetworkIdentifierWithSov(identifier)
        } else {
            val walletData = getWalletExtendedInformation(identifier)
            token = walletData.walletToken
            modifiedDid = replaceNetworkIdentifierWithSov(walletData.did)
        }
        val didDocResult = acaPyService.resolveDidDoc(modifiedDid, token)
        val resolutionResultAsJson = Json.encodeToString(ResolutionResult.serializer(), didDocResult)
        val res: ResolutionResult = Json.decodeFromString(replaceSovWithNetworkIdentifier(resolutionResultAsJson))
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

    override suspend fun issuePresentation(vpRequest: VerifiablePresentationRequestDto): VerifiablePresentationDto {
        log.debug("Issue Presentation $vpRequest")
        val holderWalletData = getWalletExtendedInformation(vpRequest.holderIdentifier)
        val holderDid = holderWalletData.did
        val token = holderWalletData.walletToken
        val verificationMethod = getVerificationMethod(vpRequest.holderIdentifier, 0)
        val signRequest: SignRequest<VerifiablePresentationDto> = SignRequest(
            doc = SignDoc(
                credential = VerifiablePresentationDto(
                    id = UUID.randomUUID().toString(),
                    context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
                    type = listOf("VerifiablePresentation"),
                    holder = holderDid,
                    verifiableCredential = vpRequest.verifiableCredentials,
                ),
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = replaceSovWithNetworkIdentifier(verificationMethod.id)
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

    override suspend fun addService(identifier: String, serviceDto: DidServiceDto): DidDocumentDto {
        log.debug("Add Service Endpoint for $identifier")
        checkSupportedId(serviceDto.id)
        val walletData = getWalletExtendedInformation(identifier)
        if (!isCatenaXWallet(walletData.bpn)) {
            throw NotImplementedException("Add Service Endpoint is not supported for the given wallet $identifier")
        }
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            didDoc.services.map {
                if (it.id.split("#")[1] == serviceDto.id) {
                    throw ConflictException("Service end point already exists")
                }
            }
        }
        acaPyService.updateService(
            DidEndpointWithType(
                didIdentifier = getIdentifierOfDid(walletData.did),
                endpoint = serviceDto.serviceEndpoint,
                endpointType = mapServiceTypeToEnum(serviceDto.type)
            ),
            walletData.walletToken
        )
        return resolveDocument(walletData.did)
    }

    override suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ): DidDocumentDto {
        log.debug("Update Service Endpoint for $identifier")
        checkSupportedId(id)
        val walletData = getWalletExtendedInformation(identifier)
        if (!isCatenaXWallet(walletData.bpn)) {
            throw NotImplementedException("Update Service Endpoint is not supported for the wallet $identifier")
        }
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            var found = false
            didDoc.services.map {
                if (it.id.split("#")[1] == id) {
                    acaPyService.updateService(
                        DidEndpointWithType(
                            didIdentifier = getIdentifierOfDid(walletData.did),
                            endpoint = serviceUpdateRequestDto.serviceEndpoint,
                            endpointType = mapServiceTypeToEnum(serviceUpdateRequestDto.type)
                        ),
                        walletData.walletToken
                    )
                    found = true
                }
            }
            if (found) {
                return resolveDocument(walletData.did)
            }
            throw NotFoundException("Target Service Endpoint not Found")
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

    override fun isCatenaXWallet(bpn: String): Boolean = bpn == baseWalletBpn

    override fun getCatenaXBpn(): String = baseWalletBpn

    override suspend fun verifyVerifiablePresentation(vpDto: VerifiablePresentationDto,
                                                      withDateValidation: Boolean): VerifyResponse {
        val catenaXWallet = getWalletExtendedInformation(baseWalletBpn)
        validateVerifiablePresentation(vpDto, catenaXWallet.walletToken)

        val listOfVerifiableCredentials = vpDto.verifiableCredential
        if (!listOfVerifiableCredentials.isNullOrEmpty()) {
            listOfVerifiableCredentials.forEach {
                validateVerifiableCredential(it, withDateValidation, catenaXWallet.walletToken)
            }
        }
        return VerifyResponse(error = null, valid = true, vp = vpDto)
    }

    override fun isDID(identifier: String) : Boolean = identifier.startsWith("did:")

    override fun getDidFromBpn(bpn: String): String = getWallet(bpn, false).did

    override fun getBpnFromDid(did: String): String = getWallet(did, false).bpn

    override fun getBpnFromIdentifier(identifier: String): String {
        return if (isDID(identifier)) {
            getWallet(identifier).bpn
        } else {
            identifier
        }
    }

    private suspend fun validateVerifiableCredential(
        vc: VerifiableCredentialDto,
        withDateValidation: Boolean,
        walletToken: String
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
        walletToken: String
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

    private fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun getIdentifierOfDid(did: String): String {
        val elementsOfDid: List<String> = did.split(":")
        return elementsOfDid[elementsOfDid.size - 1]
    }

    private fun mapServiceTypeToEnum(type: String): String = when (type) {
        "did-communication" -> EndPointType.Endpoint.name
        "linked_domains" -> EndPointType.LinkedDomains.name
        "profile" -> EndPointType.Profile.name
        else -> throw NotImplementedException("Service type $type is not supported")
    }

    private fun checkSupportedId(id: String) {
        if (!arrayOfSupportedIds.contains(id)) {
            throw NotImplementedException("The Id $id of the service is not supported")
        }
    }

    private fun replaceSovWithNetworkIdentifier(input: String): String =
        input.replace(":sov:", ":indy:$networkIdentifier:")

    private fun replaceNetworkIdentifierWithSov(input: String): String =
        input.replace(":indy:$networkIdentifier:", ":sov:")

}

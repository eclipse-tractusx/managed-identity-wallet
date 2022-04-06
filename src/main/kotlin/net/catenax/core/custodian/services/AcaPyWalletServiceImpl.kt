package net.catenax.core.custodian.services

import foundation.identity.jsonld.JsonLDUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.*
import net.catenax.core.custodian.models.ssi.acapy.*
import net.catenax.core.custodian.persistence.repositories.CredentialRepository
import net.catenax.core.custodian.persistence.repositories.WalletRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class AcaPyWalletServiceImpl(
    private val acaPyService: IAcaPyService,
    private val walletRepository: WalletRepository,
    private val credentialRepository: CredentialRepository
) : WalletService {

    private val networkIdentifier = acaPyService.getWalletAndAcaPyConfig().networkIdentifier
    private val catenaXMainBpn = acaPyService.getWalletAndAcaPyConfig().catenaXBpn
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
                WalletDto(walletDto.name, walletDto.bpn, walletDto.did, walletDto.verKey, walletDto.createdAt, credentials)
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
            walletName = walletCreateDto.bpn + "-" + JsonLDUtils.dateToString(Date()),
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
        // run Async
        issueAndStoreBpnCredentialsAsync(storedWallet.bpn)
        // run Async
        if (!isCatenaXWallet(walletCreateDto.bpn)) {
            issueAndStoreMembershipCredentialsAsync(storedWallet.bpn)
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
        val catenaXWallet = getWalletExtendedInformation(catenaXMainBpn)
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
            issuerIdentifier = catenaXMainBpn,
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
        val convertedDatetime: Date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
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
        val walletData = getWalletExtendedInformation(identifier)
        val modifiedDid = replaceNetworkIdentifierWithSov(walletData.did)
        val didDocResult = acaPyService.resolveDidDoc(modifiedDid, walletData.walletToken)
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
        checkHolderOfCredentials(holderDid, vpRequest.verifiableCredentials)
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

    private fun checkHolderOfCredentials(holderDid: String, vcs: List<VerifiableCredentialDto>) {
        vcs.map {
            if (it.credentialSubject["id"] == null || it.credentialSubject["id"] != holderDid) {
                throw ForbiddenException("Only holders are allowed to use the verifiable credentials")
            }
        }
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

    private fun isCatenaXWallet(bpn: String): Boolean = bpn == catenaXMainBpn

    private suspend fun issueAndStoreBpnCredentialsAsync(bpn: String): Deferred<Boolean>  = GlobalScope.async {
        try {
            val vcr = VerifiableCredentialRequestWithoutIssuerDto(
                id = UUID.randomUUID().toString(),
                context= listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_BPN_CREDENTIALS
                ),
                type = listOf(JsonLdTypes.BPN_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
                issuanceDate = JsonLDUtils.dateToString(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())),
                credentialSubject = mapOf(
                    "type" to listOf(JsonLdTypes.BPN_TYPE),
                    "bpn" to bpn
                ),
                holderIdentifier = bpn
            )
            val issuedVC: VerifiableCredentialDto= issueCatenaXCredential(vcr)
            if (issuedVC.proof != null){
                storeCredential(bpn, IssuedVerifiableCredentialRequestDto(
                    id = issuedVC.id,
                    type = issuedVC.type,
                    context = issuedVC.context,
                    issuer = issuedVC.issuer,
                    issuanceDate = issuedVC.issuanceDate,
                    expirationDate = issuedVC.expirationDate,
                    credentialSubject = issuedVC.credentialSubject,
                    proof = issuedVC.proof
                ))
                return@async true
            }
            log.error("Error: Proof of Bpn Credential is empty")
            false
        } catch (e: Exception) {
            log.error("Error: IssueAndStoreBpnCredentialsAsync failed with message ${e.message}")
            false
        }
    }

    private suspend fun issueAndStoreMembershipCredentialsAsync(bpn: String): Deferred<Boolean>  = GlobalScope.async {
        try {
            val currentDateAsString = JsonLDUtils.dateToString(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            val vcr = VerifiableCredentialRequestWithoutIssuerDto(
                id = UUID.randomUUID().toString(),
                context= listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_MEMBERSHIP_CREDENTIALS
                ),
                type = listOf(JsonLdTypes.MEMBERSHIP_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
                issuanceDate = currentDateAsString,
                credentialSubject = mapOf(
                    "type" to listOf(JsonLdTypes.MEMBERSHIP_TYPE),
                    "bpn" to bpn,
                    "memberOf" to "Catena-X",
                    "status" to "Active",
                    "startTime" to currentDateAsString
                ),
                holderIdentifier = bpn
            )
            val issuedVC: VerifiableCredentialDto = issueCatenaXCredential(vcr)
            if (issuedVC.proof != null) {
                storeCredential(
                    bpn,
                    IssuedVerifiableCredentialRequestDto(
                        id = issuedVC.id,
                        type = issuedVC.type,
                        context = issuedVC.context,
                        issuer = issuedVC.issuer,
                        issuanceDate = issuedVC.issuanceDate,
                        expirationDate = issuedVC.expirationDate,
                        credentialSubject = issuedVC.credentialSubject,
                        proof = issuedVC.proof
                    )
                )
                return@async true
            }
            log.error("Error: Proof of Membership Credential is empty")
            false
        } catch (e: Exception) {
            log.error("Error: IssueAndStoreMembershipCredentialsAsync failed with message ${e.message}")
            false
        }
    }
}

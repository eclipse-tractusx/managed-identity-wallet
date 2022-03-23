package net.catenax.core.custodian.services

import foundation.identity.jsonld.JsonLDUtils
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

class WalletAcaPyServiceImpl(
    private val acaPyService: IAcaPyService,
    private val walletRepository: WalletRepository,
    private val credentialRepository: CredentialRepository
) : WalletService {

    private val networkIdentifier = acaPyService.getNetworkIdentifier()
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

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
                WalletDto(walletDto.name, walletDto.bpn, walletDto.did, walletDto.createdAt, credentials)
            }
            walletDto
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
        val subWalletToCreate = CreateSubWallet(
            keyManagementMode = KeyManagementMode.MANAGED.toString(),
            label = walletCreateDto.name,
            walletWebhookUrls = emptyList(),
            walletDispatchType = WalletDispatchType.BASE.toString(),
            walletKey = createRandomString(),
            walletName = walletCreateDto.bpn + "-" + JsonLDUtils.dateToString(Date()),
            walletType = WalletType.ASKAR.toString()
        )
        // Create sub Wallet on ledger
        val createdSubWalletDto = acaPyService.createSubWallet(subWalletToCreate)
        // Create local DID on ledger
        val createdDid = acaPyService.createLocalDidForWallet(
            DidCreate(method = DidMethod.SOV.toString(), options = DidCreateOptions(KeyType.ED25519.toString())),
            createdSubWalletDto.token
        )
        acaPyService.getTokenByWalletIdAndKey(createdSubWalletDto.walletId, subWalletToCreate.walletKey)
        // Register DID on ledger
        acaPyService.registerDidOnLedger(
            DidRegistration(
                alias = walletCreateDto.name,
                did = createdDid.result.did,
                verkey = createdDid.result.verkey
            )
        )
        // set as Public
        acaPyService.assignDidToPublic(
            getIdentifierOfDid(createdDid.result.did),
            createdSubWalletDto.token
        )
        val walletToCreate = WalletExtendedData(
            name = walletCreateDto.name,
            bpn = walletCreateDto.bpn,
            did = "did${networkIdentifier}${createdDid.result.did}",
            walletId = createdSubWalletDto.walletId,
            walletKey = subWalletToCreate.walletKey,
            walletToken = createdSubWalletDto.token
        )
        return transaction {
            val createdWalletData = walletRepository.addWallet(walletToCreate)
            walletRepository.toObject(createdWalletData)
        }
    }

    override suspend fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        val walletData = getWalletPrivateInformation(identifier)
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

    override suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        log.debug("Issue Credential $vcRequest")
        val issuerWalletData = getWalletPrivateInformation(vcRequest.issuerIdentifier)
        val holderWalletData = getWalletPrivateInformation(vcRequest.holderIdentifier)

        val issuerDid = issuerWalletData.did
        val holderDid = holderWalletData.did

        val credentialSubject = vcRequest.credentialSubject.toMutableMap()
        if (credentialSubject["id"] != null || credentialSubject["id"] != holderDid) {
            credentialSubject["id"] = holderDid
        }
        val verKey = getVerificationKey(issuerDid, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
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
                    verificationMethod = "$issuerDid#key-1"
                )
            ),
            verkey = verKey
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
        val walletData = getWalletPrivateInformation(identifier)
        val modifiedDid = walletData.did.replace(networkIdentifier, ":sov:")
        val didDocResult = acaPyService.resolveDidDoc(modifiedDid, walletData.walletToken)
        val json = Json.encodeToString(ResolutionResult.serializer(), didDocResult)
        val res: ResolutionResult = Json.decodeFromString(json.replace(":sov:", networkIdentifier))
        return res.didDoc
    }

    private suspend fun getVerificationKey(identifier: String, type: String): String {
        log.debug("Get Verification Key $identifier")
        val didDocumentDto = resolveDocument(identifier)
        if (!didDocumentDto.verificationMethods.isNullOrEmpty()) {
            return when (type) {
                VerificationKeyType.PUBLIC_KEY_BASE58.toString() -> didDocumentDto.verificationMethods[0].publicKeyBase58
                    ?: throw BadRequestException("Verification Key with publicKeyBase58 does not exits")
                else -> {
                    throw BadRequestException("Not supported type")
                }
            }
        }
        throw BadRequestException("Get Verification key exception")
    }

    override suspend fun issuePresentation(vpRequest: VerifiablePresentationRequestDto): VerifiablePresentationDto {
        log.debug("Issue Presentation $vpRequest")
        val holderWalletData = getWalletPrivateInformation(vpRequest.holderIdentifier)
        val holderDid = holderWalletData.did
        val token = holderWalletData.walletToken
        val verKey = getVerificationKey(holderDid, VerificationKeyType.PUBLIC_KEY_BASE58.toString())
        val signRequest: SignRequest<VerifiablePresentationDto> = SignRequest(
            doc = SignDoc(
                credential = VerifiablePresentationDto(
                    id = UUID.randomUUID().toString(),
                    context = listOf("https://www.w3.org/2018/credentials/v1"),
                    type = listOf("VerifiablePresentation"),
                    holder = holderDid,
                    verifiableCredential = vpRequest.verifiableCredentials,
                ),
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = "$holderDid#key-1"
                )
            ),
            verkey = verKey
        )
        val signedVpAsJsonString = acaPyService.signJsonLd(signRequest, token)
        val signedVpResult: SignPresentationResponse = Json.decodeFromString(signedVpAsJsonString)
        if (signedVpResult.signedDoc != null) {
            return signedVpResult.signedDoc
        }
        if (signedVpResult.error != null) {
            throw BadRequestException(signedVpResult.error)
        }
        throw BadRequestException("Issuer presentation failed!")
    }

    override suspend fun addService(identifier: String, serviceDto: DidServiceDto): DidDocumentDto {
        log.debug("Add Service Key $identifier")
        val walletData = getWalletPrivateInformation(identifier)
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            didDoc.services.map {
                if (it.type == serviceDto.type) {
                    throw ConflictException("Service end point already exists")
                }
            }
        }
        val didEndpointWithType = DidEndpointWithType(
            didIdentifier = getIdentifierOfDid(walletData.did),
            endpoint = serviceDto.serviceEndpoint,
            endpointType = mapServiceTypeToEnum(serviceDto.type)
        )
        acaPyService.updateService(didEndpointWithType, walletData.walletToken)
        return resolveDocument(walletData.did)
    }

    override suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ): DidDocumentDto {
        log.debug("Add Service Key $identifier")
        val walletData = getWalletPrivateInformation(identifier)
        val didDoc = resolveDocument(walletData.did)
        if (!didDoc.services.isNullOrEmpty()) {
            var found = false
            didDoc.services.map {
                if (it.type == serviceUpdateRequestDto.type) {
                    val didEndpointWithType = DidEndpointWithType(
                        didIdentifier = getIdentifierOfDid(walletData.did),
                        endpoint = serviceUpdateRequestDto.serviceEndpoint,
                        endpointType = mapServiceTypeToEnum(serviceUpdateRequestDto.type)
                    )
                    acaPyService.updateService(didEndpointWithType, walletData.walletToken)
                    found = true
                }
            }
            if (found) {
                return resolveDocument(walletData.did)
            }
            throw BadRequestException("Not Found")
        }
        throw BadRequestException("Get Verification key exception")
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
        log.debug("Get Credentials")
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
        val listOfCredentials =
            credentialRepository.getCredentials(issuerDid, holderDid, type, credentialId)
        if (listOfCredentials.isNotEmpty()) {
            return listOfCredentials.map { credentialRepository.fromRow(it) }
        }
        return emptyList()
    }

    private fun getWalletPrivateInformation(identifier: String): WalletExtendedData {
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            walletRepository.toWalletCompleteDataObject(extractedWallet)
        }
    }

    private fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom.getInstanceStrong().nextInt(0, charPool.size) }
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
}

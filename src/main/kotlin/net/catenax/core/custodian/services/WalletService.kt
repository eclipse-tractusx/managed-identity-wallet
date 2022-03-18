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

class WalletService(
    private val walletRepository: WalletRepository,
    private val credentialRepository: CredentialRepository) {

    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val acaPyService = AcaPyService.create()

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

     fun getWallet(identifier: String): WalletDto {
        log.debug("Get Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            walletRepository.toObject(extractedWallet)
        }
    }

     fun getAll(): List<WalletDto> {
        log.debug("Get All Wallets")
        return transaction {
            val listOfWallets = walletRepository.getAll()
            listOfWallets.map { walletRepository.toObject(it) }
        }
    }

     suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
            log.debug("Add a new Wallet with bpn ${walletCreateDto.bpn}")
            val subWalletToCreate = CreateSubWallet(
                keyManagementMode =  "managed",
                label =  walletCreateDto.name,
                walletWebhookUrls =  listOf("http://localhost:8022/webhooks"),
                walletDispatchType =  "default",
                walletKey =  createRandomString(25),
                walletName =  walletCreateDto.bpn + "-" + JsonLDUtils.dateToString(Date()),
                walletType = "indy"
            )
            val createdSubWalletDto = acaPyService.createSubWallet(subWalletToCreate)
            val createdDid = acaPyService.createLocalDidForWallet(
                DidCreate(method = "sov", options = DidCreateOptions(keyType = "ed25519")),
                createdSubWalletDto.token
            )
            acaPyService.getTokenByWalletIdAndKey(createdSubWalletDto.walletId, subWalletToCreate.walletKey)
            acaPyService.registerDidOnLedger(
                DidRegistration(
                    alias = walletCreateDto.name,
                    did = createdDid.result.did,
                    verkey = createdDid.result.verkey
                )
            )
            val walletToCreate = WalletData(
                name = walletCreateDto.name,
                bpn = walletCreateDto.bpn,
                did= "did:indy:" + createdDid.result.did,
                walletId =  createdSubWalletDto.walletId,
                walletKey = subWalletToCreate.walletKey,
                walletToken = createdSubWalletDto.token
            )
            return transaction {
                val createdWalletData = walletRepository.addWallet(walletToCreate)
                walletRepository.toObject(createdWalletData)
            }
    }

    fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        return transaction {
            walletRepository.deleteWallet(identifier)
        }
    }

    fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean {
        log.debug("Store Credential in Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            val credentialAsJson = Json.encodeToString(IssuedVerifiableCredentialRequestDto.serializer(), issuedCredential)
            val sortedTypes = issuedCredential.type.sorted()
            credentialRepository.storeCredential(
                issuedCredential,
                credentialAsJson,
                sortedTypes.joinToString(","),
                extractedWallet)
            true
        }
    }

    suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto {
        val issuerWalletData = getWalletInformation(vcRequest.issuerIdentifier)
        val holderWalletData = getWalletInformation(vcRequest.holderIdentifier)

        val issuerDid = issuerWalletData.did
        // TODO Check is Credential Subject has the holder's DID or BPN and replace it if needed
        val holderDid = holderWalletData.did
        val verKey = getVerificationKey(issuerDid, "publicKeyBase58")
        val convertedDatetime: Date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
        val issuanceDate = vcRequest.issuanceDate ?: JsonLDUtils.dateToString(convertedDatetime)
        val signRequest: SignRequest<VerifiableCredentialDto> = SignRequest(
            SignDoc(
                credential = VerifiableCredentialDto(
                    id = vcRequest.id,
                    context = vcRequest.context,
                    type = vcRequest.type,
                    issuer =  issuerDid,
                    issuanceDate = issuanceDate,
                    credentialSubject = vcRequest.credentialSubject,
                    expirationDate = vcRequest.expirationDate
                ),
                options = SignOptions(
                    proofPurpose = "assertionMethod",
                    type = "Ed25519Signature2018",
                    verificationMethod = "$issuerDid#key-1")
                ),
            verkey = verKey
        )

        val signedVcResultAsJsonString = acaPyService.signCredentialJsonLd(signRequest, issuerWalletData.walletToken)
        val signedVcResult: SignCredentialResponse = Json.decodeFromString(signedVcResultAsJsonString)
        if (signedVcResult.signedDoc != null) {
            val verifyRequest: VerifyRequest<VerifiableCredentialDto> = VerifyRequest(
                signedDoc = signedVcResult.signedDoc,
                verkey = verKey
            )
            // remove later if not needed
            val result = acaPyService.verifyJsonLd(verifyRequest, issuerWalletData.walletToken)
            if (result.valid) {
                return signedVcResult.signedDoc
            }
        }
        throw BadRequestException(signedVcResult.error)
    }

    suspend fun resolveDocument(identifier: String): DidDocumentDto {
        val walletData = getWalletInformation(identifier)
        val didDocResult = acaPyService.resolveDidDoc(walletData.did, walletData.walletToken)
        val json = Json.encodeToString(ResolutionResult.serializer(), didDocResult)
        val res: ResolutionResult = Json.decodeFromString(json.replace(":sov:", ":indy:"))
        return res.didDoc
    }

    private suspend fun getVerificationKey(identifier: String, type: String): String {
        val didDocumentDto = resolveDocument(identifier)
        if (!didDocumentDto.verificationMethods.isNullOrEmpty()) {
            return when (type) {
                "publicKeyBase58" -> didDocumentDto.verificationMethods[0].publicKeyBase58
                    ?: throw BadRequestException("publicKeyBase58 does not exits")
                else -> {
                    throw BadRequestException("Not supported type")
                }
            }
        }
        throw BadRequestException("Get Verification key exception")
    }

    suspend fun issuePresentation(vpRequest: VerifiablePresentationRequestDto): VerifiablePresentationDto {
        val holderWalletData = getWalletInformation(vpRequest.holderIdentifier)
        val holderDid = holderWalletData.did
        val token = holderWalletData.walletToken
        val verKey = getVerificationKey(holderDid, "publicKeyBase58")
        val signRequest: SignRequest<VerifiablePresentationDto> = SignRequest(
                doc = SignDoc(
                    credential = VerifiablePresentationDto(
                        id = "http://example.edu/credentials/373234",
                        context = listOf("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
                        type= listOf("VerifiablePresentation"),
                        holder = holderDid,
                        verifiableCredential = vpRequest.verifiableCredentials,
                    ),
                    options = SignOptions(
                        proofPurpose = "assertionMethod",
                        type = "Ed25519Signature2018",
                        verificationMethod = "$holderDid#key-1")
                ),
                verkey = verKey
            )

        // val signedPresentationRes = acaPyService.signPresentationJsonLd(signRequest, token)
        val signedVpAsJsonString  =  acaPyService.signCredentialJsonLd(signRequest, token)
        val signedVpResult: SignPresentationResponse = Json.decodeFromString(signedVpAsJsonString)
        if (signedVpResult.signedDoc != null) {
            return signedVpResult.signedDoc
        }
        if (signedVpResult.error != null) {
            throw BadRequestException(signedVpResult.error)
        }
        throw BadRequestException("Issuer presentation failed!")
    }

    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?): List<VerifiableCredentialDto> {
        val listOfCredentials = credentialRepository.getCredentials(issuerIdentifier, holderIdentifier, type, credentialId)
        if (listOfCredentials.isNotEmpty()) {
            return listOfCredentials.map { credentialRepository.fromRow(it) }
        }
        return emptyList()
    }

    private fun getWalletInformation(identifier: String): WalletData {
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            walletRepository.toWalletCompleteDataObject(extractedWallet)
        }
    }

    private fun createRandomString(length: Int): String {
        return (1..length)
            .map { SecureRandom.getInstanceStrong().nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}

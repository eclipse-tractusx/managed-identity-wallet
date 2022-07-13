package net.catenax.core.managedidentitywallets

import net.catenax.core.managedidentitywallets.models.*
import net.catenax.core.managedidentitywallets.models.ssi.DidDocumentDto
import net.catenax.core.managedidentitywallets.models.ssi.DidVerificationMethodDto
import net.catenax.core.managedidentitywallets.models.ssi.acapy.*

import net.catenax.core.managedidentitywallets.services.IAcaPyService
import java.security.SecureRandom

class AcaPyMockedService: IAcaPyService {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private var currentDid: String = "EXAMPLE"

    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        return WalletAndAcaPyConfig(
            apiAdminUrl = "",
            networkIdentifier = "local:test",
            baseWalletBpn = "bpn1",
            adminApiKey = "Hj23iQUsstG!dde"
        )
    }

    override suspend fun getWallets(): WalletList = WalletList(results = emptyList())

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        return CreatedSubWalletResult(
            createdAt = "createdAt",
            walletId = "walletId",
            keyManagementMode = "managed",
            updatedAt = "updatedAt",
            WalletSettings(
                walletType = "walletType",
                walletName = "walletName",
                walletWebhookUrls = emptyList(),
                walletDispatchType = "walletDispatchType",
                walletId = "walletId",
                defaultLabel = "defaultLabel",
                imageUrl = "imageUrl"
            ),
            token = "token"
        )
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String) {
        if (didIdentifier.contains(getWalletAndAcaPyConfig().networkIdentifier)) {
            throw Exception("Cannot process did containing network identifier!")
        }
        if (didIdentifier.indexOf(":") == 0) {
            throw Exception("Cannot process did starting with a colon!")
        }
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {}

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse =
        CreateWalletTokenResponse(token = "token")

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        currentDid = createRandomString()
        return DidResult(
            result = DidResultDetails(
                did = currentDid,
                keyType = "",
                method = "",
                posture = "",
                verkey = "abc"
            )
        )
    }

    override suspend fun registerDidOnLedger(
        didRegistration: DidRegistration,
        endorserWalletToken: String
    ): DidRegistrationResult {
        if (didRegistration.did.contains(getWalletAndAcaPyConfig().networkIdentifier)) {
            throw Exception("Cannot process did containing network identifier!")
        }
        if (didRegistration.did.indexOf(":") == 0) {
            throw Exception("Cannot process did starting with a colon!")
        }
        return DidRegistrationResult(success = true)
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String = ""

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse =
        VerifyResponse(error = null, valid = true)

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult {
        if (did == "did:sov:AA5EEDcn8yTfMobaTcabj9"){
            return ResolutionResult(
                didDoc = DidDocumentDto(
                    id = did,
                    context = emptyList(),
                    verificationMethods = listOf(
                        DidVerificationMethodDto(
                            id = "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                            type = "Ed25519VerificationKey2018",
                            controller = "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                            publicKeyBase58= "5zTG9qLF8DEzR7fmCa9jy6L5Efi5QvpWEvMXszh9jStA"
                        )
                    )
                ),
                metadata = ResolutionMetaData(resolverType = "", resolver = "", retrievedTime = "", duration = 0)
            )
        }
        if (did == "did:sov:LCNSw1JxSTDw7EpR1UMG7D") {
            return ResolutionResult(
                didDoc = DidDocumentDto(
                    id = did,
                    context = emptyList(),
                    verificationMethods = listOf(
                        DidVerificationMethodDto(
                            id = "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                            type = "Ed25519VerificationKey2018",
                            controller = "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D",
                            publicKeyBase58= "BTppBmURHHqg6PKf7ryv8VS7hrKg8nEhwmjuD9ciGssz"
                        )
                    )
                ),
                metadata = ResolutionMetaData(resolverType = "", resolver = "", retrievedTime = "", duration = 0)
            )
        }
        if (did == "did:sov:M6Mis1fZKuhEw71GNY3TAb") {
            return ResolutionResult(
                didDoc = DidDocumentDto(
                    id = did,
                    context = emptyList(),
                    verificationMethods = listOf(
                        DidVerificationMethodDto(
                            id = "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb#key-1",
                            type = "Ed25519VerificationKey2018",
                            controller = "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb",
                            publicKeyBase58= "BxAExpSNdKQ4KA7ocjH7qgphkbdKva8kKy1pDn5ksWxV"
                        )
                    )
                ),
                metadata = ResolutionMetaData(resolverType = "", resolver = "", retrievedTime = "", duration = 0)
            )
        }
        if (did == "did:sov:YHXZLLSLnKxz5D2HQaKXcP"){
            return ResolutionResult(
                didDoc = DidDocumentDto(
                    id = did,
                    context = emptyList(),
                    verificationMethods = listOf(
                        DidVerificationMethodDto(
                            id = "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP#key-1",
                            type = "Ed25519VerificationKey2018",
                            controller = "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP",
                            publicKeyBase58= "J3ymiVmkB6yEWZ9qsp62kHzGmGm2phdvapRA6bkoJmBW"
                        )
                    )
                ),
                metadata = ResolutionMetaData(resolverType = "", resolver = "", retrievedTime = "", duration = 0)
            )
        }
        return ResolutionResult(
            didDoc = DidDocumentDto(id = did, context = emptyList()),
            metadata = ResolutionMetaData(
                resolverType = "",
                resolver = "",
                retrievedTime = "",
                duration = 0
            )
        )
    }

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String) {}

    private fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}

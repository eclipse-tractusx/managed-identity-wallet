package net.catenax.core.custodian

import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.DidDocumentDto
import net.catenax.core.custodian.models.ssi.acapy.*

import net.catenax.core.custodian.services.IAcaPyService
import java.security.SecureRandom

class AcaPyMockedService(): IAcaPyService {

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    var currentDid: String = "EXAMPLE";

    override fun getNetworkIdentifier(): String = "local:test"

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

    override suspend fun assignDidToPublic(didIdentifier: String, token: String) {}

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

    override suspend fun registerDidOnLedger(didRegistration: DidRegistration): DidRegistrationResult =
        DidRegistrationResult(seed = null, did = didRegistration.did, verkey = didRegistration.verkey)

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String = ""

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse =
        VerifyResponse(error = null, valid = true)

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult =
        ResolutionResult(
            didDoc = DidDocumentDto(id = did, context = emptyList()),
            metadata = ResolutionMetaData(
                resolverType = "",
                resolver = "",
                retrievedTime = "",
                duration = 0
            )
        )

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String) {}

    private fun createRandomString(): String {
        return (1..25)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}

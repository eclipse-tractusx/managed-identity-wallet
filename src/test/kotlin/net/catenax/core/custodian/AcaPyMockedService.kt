package net.catenax.core.custodian

import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.acapy.*
import net.catenax.core.custodian.routes.didDocumentDtoExample

import net.catenax.core.custodian.services.IAcaPyService

class AcaPyMockedService(): IAcaPyService {

    override fun getNetworkIdentifier(): String = ""

    override suspend fun getWallets(): WalletList = WalletList(results = emptyList())

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        return CreatedSubWalletResult(
            createdAt = "",
            walletId = "",
            keyManagementMode = "",
            updatedAt = "",
            WalletSettings(
                walletType = "",
                walletName = "",
                walletWebhookUrls = emptyList(),
                walletDispatchType = "",
                walletId = "",
                defaultLabel = "",
                imageUrl = ""
            ),
            token = ""
        )
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String): Boolean = true

    override suspend fun deleteSubWallet(walletData: WalletExtendedData): Boolean = true

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse =
        CreateWalletTokenResponse(token = "")

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult =
        DidResult(
            result = DidResultDetails(
                did = "",
                keyType = "",
                method = "",
                posture = "",
                verkey = ""
            )
        )

    override suspend fun registerDidOnLedger(didRegistration: DidRegistration): DidRegistrationResult =
        DidRegistrationResult(seed = "", did = "", verkey = "")

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String = ""

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse =
        VerifyResponse(error = null, valid = true)

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult =
        ResolutionResult(
            didDoc = didDocumentDtoExample["demo"]!!,
            metadata = ResolutionMetaData(
                resolverType = "",
                resolver = "",
                retrievedTime = "",
                duration = 0
            )
        )

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String): Boolean = true
}

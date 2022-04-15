package net.catenax.core.custodian.services

import io.ktor.client.*
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.acapy.*

interface IAcaPyService {

    fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig

    suspend fun getWallets(): WalletList

    suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult

    suspend fun assignDidToPublic(didIdentifier: String, token: String)

    suspend fun deleteSubWallet(walletData: WalletExtendedData)

    suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse

    suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult

    suspend fun registerDidOnLedger(didRegistration: DidRegistration, endorserWalletToken: String): DidRegistrationResult

    suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String

    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse

    suspend fun resolveDidDoc(did: String, token: String): ResolutionResult

    suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String)

    companion object {
        fun create(walletAndAcaPyConfig: WalletAndAcaPyConfig, client: HttpClient): IAcaPyService {
            return AcaPyService(walletAndAcaPyConfig, client)
        }
    }
}

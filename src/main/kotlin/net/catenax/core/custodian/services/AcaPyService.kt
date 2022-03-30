package net.catenax.core.custodian.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.acapy.*

class AcaPyService(private val acaPyConfig: WalletAndAcaPyConfig, private val client: HttpClient): IAcaPyService {

    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        return acaPyConfig
    }

    override suspend fun getWallets(): WalletList {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/multitenancy/wallets")
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = subWallet
        }
        return Json.decodeFromString(httpResponse.readText())
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/public?did=$didIdentifier")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/${walletData.walletId}/remove")
            contentType(ContentType.Application.Json)
            body = WalletKey(walletData.walletKey)
        }
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/$id/token")
            contentType(ContentType.Application.Json)
            body = WalletKey(key)
        }
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): LocalDidResult {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/create")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = didCreateDto
        }
    }

    override suspend fun getPublicDidOfWallet(tokenOfWallet: String): DidResult {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/wallet/did/public")
                headers.append(HttpHeaders.Authorization, "Bearer $tokenOfWallet")
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun registerDidOnLedger(
        didRegistration: DidRegistration,
        endorserWalletToken: String
    ): DidRegistrationResult {
        return client.post {
            // Role is ignored because endorser cannot register nym with role other than NONE
            url("${acaPyConfig.apiAdminUrl}/ledger/" +
                    "register-nym?" +
                    "did=${didRegistration.did}&" +
                    "verkey=${didRegistration.verkey}&" +
                    "alias=${didRegistration.alias}")
            headers.append(HttpHeaders.Authorization, "Bearer $endorserWalletToken")
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/sign")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = signRequest
        }
        return httpResponse.readText()
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/verify")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = verifyRequest
        }
    }

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult {
        return client.get {
            url("${acaPyConfig.apiAdminUrl}/resolver/resolve/$did")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/set-did-endpoint")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
    }

}

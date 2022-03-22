package net.catenax.core.custodian.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.acapy.*

class AcaPyService(private val acaPyConfig: AcaPyConfig, private val client: HttpClient): IAcaPyService {

    override fun getNetworkIdentifier(): String {
        return acaPyConfig.networkIdentifier
    }

    override suspend fun getWallets(): WalletList {
        return try {
            client.get {
                headers.append(HttpHeaders.Accept, "application/json")
                url("${acaPyConfig.apiAdminUrl}/multitenancy/wallets")
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        println(acaPyConfig.apiAdminUrl)
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet")
            contentType(ContentType.Application.Json)
            body = subWallet
        }
        return Json.decodeFromString(httpResponse.readText())
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String): Boolean {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/public?did=$didIdentifier")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
        return true
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData): Boolean {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/${walletData.walletId}/remove")
            contentType(ContentType.Application.Json)
            body = WalletKey(walletData.walletKey)
        }
        return true
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/$id/token")
            contentType(ContentType.Application.Json)
            body = WalletKey(key)
        }
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/create")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = didCreateDto
        }
    }

    override suspend fun registerDidOnLedger(didRegistration: DidRegistration): DidRegistrationResult {
        return client.post {
            url(acaPyConfig.ledgerUrl)
            contentType(ContentType.Application.Json)
            body = didRegistration
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

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String): Boolean {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/set-did-endpoint")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
        return true
    }

}

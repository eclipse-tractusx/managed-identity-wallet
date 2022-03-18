package net.catenax.core.custodian.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.catenax.core.custodian.models.BadRequestException
import net.catenax.core.custodian.models.ssi.acapy.*

private const val ACAPY_API_BASE_URL = "http://localhost:11000"

class AcaPyServiceImpl(private val client: HttpClient): AcaPyService {

    override suspend fun getWallets(): WalletList {
        return try {
            client.get{
                headers.append(HttpHeaders.Accept, "application/json")
                url("$ACAPY_API_BASE_URL/multitenancy/wallets")
            }
        } catch(e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        return try {
            val httpResponse: HttpResponse = client.post {
                url("$ACAPY_API_BASE_URL/multitenancy/wallet")
                contentType(ContentType.Application.Json)
                body = subWallet
            }
            if (httpResponse.status.value == 200) {
                val createdSubWalletResult: CreatedSubWalletResult = Json.decodeFromString(httpResponse.readText())
                createdSubWalletResult
            } else {
                throw BadRequestException("ERROR: " + httpResponse.status)
            }
        } catch(e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        return try {
            client.post {
                url("$ACAPY_API_BASE_URL/multitenancy/wallet/$id/token")
                contentType(ContentType.Application.Json)
                body = WalletKey(key)
            }
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        return try {
            client.post<DidResult> {
                url("$ACAPY_API_BASE_URL/wallet/did/create")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = didCreateDto
            }
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun registerDidOnLedger(didRegistration: DidRegistration): DidRegistrationResult {
        return try {
            client.post<DidRegistrationResult> {
                url("https://indy-test.bosch-digital.de/register")
                contentType(ContentType.Application.Json)
                body = didRegistration
            }
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun <T> signCredentialJsonLd(signRequest: SignRequest<T>, token: String): String {
        return try {
            val httpResponse: HttpResponse = client.post {
                url("$ACAPY_API_BASE_URL/jsonld/sign")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = signRequest
            }
            if (httpResponse.status.value == 200) {
                return httpResponse.readText()
            }
            throw BadRequestException("Failed sign request")
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun <T> signPresentationJsonLd(
    signRequest: SignRequest<T>,
    token: String
    ): SignPresentationResponse {
        return try {
            client.post<SignPresentationResponse> {
                url("$ACAPY_API_BASE_URL/jsonld/sign")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = signRequest
            }
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse {
        return try {
            client.post<VerifyResponse> {
                url("$ACAPY_API_BASE_URL/jsonld/verify")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = verifyRequest
            }
        } catch(e: Exception) {
            println("Error: ${e.message}")
            throw BadRequestException(e.message)
        }
    }

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult {
        val modifiedDid = did.replace(":indy:", ":sov:")
        return try {
            client.get<ResolutionResult> {
                url("$ACAPY_API_BASE_URL/resolver/resolve/$modifiedDid")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
        } catch(e: Exception) {
            throw BadRequestException(e.message)
       }
    }

}

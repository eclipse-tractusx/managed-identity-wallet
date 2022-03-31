package net.catenax.core.custodian.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import io.ktor.client.statement.*
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.models.ssi.*
import net.catenax.core.custodian.models.ssi.acapy.WalletAndAcaPyConfig
import net.catenax.core.custodian.persistence.repositories.CredentialRepository
import net.catenax.core.custodian.persistence.repositories.WalletRepository

interface WalletService {

    fun getWallet(identifier: String, withCredentials: Boolean = false): WalletDto

    fun getAll(): List<WalletDto>

    suspend fun createWallet(walletCreateDto: WalletCreateDto): WalletDto

    suspend fun deleteWallet(identifier: String): Boolean

    fun storeCredential(identifier: String, issuedCredential: IssuedVerifiableCredentialRequestDto): Boolean

    suspend fun issueCredential(vcRequest: VerifiableCredentialRequestDto): VerifiableCredentialDto

    suspend fun issueCatenaXCredential(vcCatenaXRequest: VerifiableCredentialRequestWithoutIssuerDto): VerifiableCredentialDto

    suspend fun resolveDocument(identifier: String): DidDocumentDto

    suspend fun issuePresentation(vpRequest: VerifiablePresentationRequestDto): VerifiablePresentationDto

    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto>

    suspend fun addService(identifier: String, serviceDto: DidServiceDto): DidDocumentDto

    suspend fun updateService(
        identifier: String,
        id: String,
        serviceUpdateRequestDto: DidServiceUpdateRequestDto
    ): DidDocumentDto

    suspend fun deleteService(identifier: String, id: String): DidDocumentDto

    companion object {
        fun createWithAcaPyService(
            walletAndAcaPyConfig: WalletAndAcaPyConfig,
            walletRepository: WalletRepository,
            credentialRepository: CredentialRepository,
        ): WalletService {
            val acaPyService = IAcaPyService.create(
                walletAndAcaPyConfig = walletAndAcaPyConfig,
                client = HttpClient() {
                    expectSuccess = true
                    install(ResponseObserver) {
                        onResponse { response ->
                            println("HTTP status: ${response.status.value}")
                            println("HTTP description: ${response.status.description}")
                        }
                    }
                    HttpResponseValidator {
                        validateResponse { response: HttpResponse ->
                            val statusCode = response.status.value
                            when (statusCode) {
                                in 300..399 -> throw RedirectResponseException(response, response.status.description)
                                in 400..499 -> throw ClientRequestException(response, response.status.description)
                                in 500..599 -> throw ServerResponseException(response, response.status.description)
                            }
                            if (statusCode >= 600) {
                                throw ResponseException(response, response.status.description)
                            }
                        }
                        handleResponseException { cause: Throwable ->
                            when (cause) {
                                is ClientRequestException -> {
                                    if ("already exists." in cause.message) {
                                        throw ConflictException("Aca-Py Error: ${cause.response.status.description}")
                                    }
                                    if ("Unprocessable Entity" in cause.message) {
                                        throw UnprocessableEntityException("Aca-Py Error: ${cause.response.status.description}")
                                    }
                                    throw cause
                                }
                                else -> throw cause
                            }
                        }
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.BODY
                    }
                    install(JsonFeature) {
                        serializer = JacksonSerializer() {
                            enable(SerializationFeature.INDENT_OUTPUT)
                            serializationConfig.defaultPrettyPrinter
                            setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        }
                    }
                }
            )
            return AcaPyWalletServiceImpl(acaPyService, walletRepository, credentialRepository)
        }
    }
}

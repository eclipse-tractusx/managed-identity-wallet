package net.catenax.core.custodian.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import net.catenax.core.custodian.models.ssi.acapy.*

interface AcaPyService {

    suspend fun getWallets(): WalletList

    suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult

    suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult

    suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse

    suspend fun registerDidOnLedger(didRegistration: DidRegistration): DidRegistrationResult

    suspend fun <T> signCredentialJsonLd(signRequest: SignRequest<T>, token: String): String

    suspend fun <T> signPresentationJsonLd(signRequest: SignRequest<T>, token: String): SignPresentationResponse

    suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse

    suspend fun resolveDidDoc(did: String, token: String): ResolutionResult

    companion object {
        fun create(): AcaPyService {
            return AcaPyServiceImpl(
                client = HttpClient() {
                    // expectSuccess = false
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
        }
    }
}
package net.catenax.core.custodian.routes

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.auth.configuration.JwtAuthConfiguration
import io.bkbn.kompendium.core.metadata.ExceptionInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import net.catenax.core.custodian.models.ExceptionResponse
import net.catenax.core.custodian.persistances.repositories.WalletRepository
import net.catenax.core.custodian.services.WalletService
import kotlin.reflect.typeOf

val semanticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input can not be processed due to semantic mismatches",
    status = HttpStatusCode.UnprocessableEntity,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val syntacticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input does not comply to the syntax requirements",
    status = HttpStatusCode.BadRequest,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

fun Application.appRoutes() {
    val walletRepository = WalletRepository()
    val walletService = WalletService(walletRepository)

    routing {
        route("/api") {

            val authConfig = object : JwtAuthConfiguration {
                override val name: String = "auth-jwt"
            }

            // based on: authenticate("auth-jwt")
            notarizedAuthenticate(authConfig) {
                walletRoutes(walletService)
                didDocRoutes()
                vcRoutes()
                vpRoutes()
            }
        }
    }
}

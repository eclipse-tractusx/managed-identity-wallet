package net.catenax.core.custodian.routes

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.auth.configuration.JwtAuthConfiguration
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ExceptionInfo
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.catenax.core.custodian.models.ExceptionResponse
import net.catenax.core.custodian.models.ssi.SignMessageDto
import net.catenax.core.custodian.models.ssi.SignMessageResponseDto
import kotlin.reflect.typeOf

val notFoundException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "Not Found",
    status = HttpStatusCode.NotFound,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

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

fun Application.ssiRoutes() {
    routing {
        route("/api") {

            val authConfig = object : JwtAuthConfiguration {
                override val name: String = "auth-jwt"
            }

            // based on: authenticate("auth-jwt")
            notarizedAuthenticate(authConfig) {
                didDocRoutes()
                vcRoutes()
                vpRoutes()
                walletRoutes()
            }
        }
    }
}

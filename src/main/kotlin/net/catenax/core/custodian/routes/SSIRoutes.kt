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

val invalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "Bad Request",
    status = HttpStatusCode.BadRequest,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

fun Application.ssiRoutes() {
    routing {
        route("/api/ssi") {

            val authConfig = object : JwtAuthConfiguration {
                override val name: String = "auth-jwt"
            }

            // based on: authenticate("auth-jwt")
            notarizedAuthenticate(authConfig) {
                didDocRoutes()
                vcRoutes()
                vpRoutes()

                route("/sign") {
                    notarizedPost(
                        PostInfo<Unit, SignMessageDto, SignMessageResponseDto>(
                            summary = "Sign A Message",
                            description = "Sign a Message using the wallet",
                            requestInfo = RequestInfo(
                                description = "the message to sign and the wallet did",
                                examples = mapOf("demo" to SignMessageDto("did1_bpn", "message_string"))
                            ),
                            responseInfo = ResponseInfo(
                                status = HttpStatusCode.Created,
                                description = "The signed message response",
                                examples = mapOf(
                                    "demo" to SignMessageResponseDto(
                                        "did", "message_string",
                                        "signed_message", "public_key"
                                    )
                                )
                            ),
                            canThrow = setOf(invalidInputException, notFoundException),
                            tags = setOf("Sign")
                        )
                    ) {
                        val signMessageDto = call.receive<SignMessageDto>()
                        val response = SignMessageResponseDto(
                            did = signMessageDto.did,
                            message = signMessageDto.message,
                            signedMessageInHex = "0x123....",
                            publicKeyBase58 = "FyfKP2HvTKqDZQzvyL38yXH7bExmwofxHf2NR5BrcGf1"
                        )
                        call.respond(HttpStatusCode.Created, response)
                    }
                }
            }
        }
    }
}

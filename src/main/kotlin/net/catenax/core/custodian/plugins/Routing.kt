package net.catenax.core.custodian.plugins

import io.ktor.http.*

// for 2.0.0-beta
// import io.ktor.server.routing.*
// import io.ktor.server.application.*
// import io.ktor.server.response.*
// import io.ktor.server.request.*

// for 1.6.7
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.sessions.*

import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.features.*
import io.ktor.client.*

import java.io.File
import java.io.IOException

import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

import io.bkbn.kompendium.core.routes.redoc


import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.persistence.repositories.WalletRepository
import net.catenax.core.custodian.services.WalletService

suspend fun retrieveBusinessPartnerInfo(datapoolUrl: String, bpn: String, token: String): String {

    var stringBody: String = ""
    HttpClient(Apache).use { client ->
        val httpResponse: HttpResponse = client.get("${datapoolUrl}/api/catena/businesspartner/${bpn}") {
            headers {
                append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                append(HttpHeaders.Authorization, "Bearer " + token)
            }
        }
        stringBody = httpResponse.readText()
    }

    return stringBody
}

@Serializable
data class BusinessPartnerInfo(val bpn: String)

fun Application.configureRouting() {
    val walletRepository = WalletRepository()
    val walletService = WalletService(walletRepository)

    val datapoolUrl = environment.config.property("datapool.url").getString()

    routing {

        redoc(pageTitle = "Custodian API Docs")

        get("/") {
            call.respondHtml {
                head {
                    title { +"Catena-X Core // Custodian" }
                    style { "body { font-family: Arial; }" }
                }
                body {
                    h1 { +"Catena-X Core // Custodian" }
                    p { a("/docs") { +"See API docs" } }
                    p { a("/ui/") { +"Admin UI" } }
                }
            }
        }

        authenticate("auth-ui") {
            get("/login") {
                // triggers automatically the login
            }
            route("/callback") {
                // This handler will be executed after making a request to a provider's token URL.
                handle {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse>()
                    if (principal != null) {
                        val response = principal as OAuthAccessTokenResponse.OAuth2
                        // TODO put claims into the session
                        // val token = JWT.decode(response.accessToken)
                        // val name = token.getClaim("preferred_username").asString()
                        call.sessions.set(UserSession(response.accessToken.toString()))
                        call.respondRedirect("/ui/")
                    } else {
                        call.respondRedirect("/login")
                    }
                }
            }
        }

        route("/ui") {

            authenticate("auth-ui-session") {
                static("/") {
                    staticRootFolder = File("/app/static")
                    files(".")
                    default("index.html")
                }

                route("/wallets") {
                    handle {

                        val userSession = call.sessions.get<UserSession>()
                        if (userSession != null) {
                            // could be used later
                        }

                        call.respond(walletService.getAll())
                    }
                }

                route("/wallets/{did}/full") {
                    handle {
                        val userSession = call.sessions.get<UserSession>()
                        var token = ""
                        if (userSession != null) {
                            token = userSession.token
                        }

                        val did = call.parameters["did"]
                        if (did != null) {
                            try {
                                val stringBody = retrieveBusinessPartnerInfo("${datapoolUrl}", did, token)
                                val d = Json { ignoreUnknownKeys = true }.decodeFromString<BusinessPartnerInfo>(
                                    BusinessPartnerInfo.serializer(),
                                    stringBody
                                )
                                call.respondText(stringBody, ContentType.Application.Json, HttpStatusCode.OK)
                            } catch (e: RedirectResponseException) {
                                log.warn("RedirectResponseException: " + e.message)
                                throw BadRequestException("Could not retrieve business partner details!")
                            } catch (e: ClientRequestException) {
                                log.warn("ClientRequestException: " + e.message)
                                throw BadRequestException("Could not retrieve business partner details!")
                            } catch (e: ServerResponseException) {
                                log.warn("ServerResponseException: " + e.message)
                                throw BadRequestException("Could not retrieve business partner details!")
                            } catch (e: SerializationException) {
                                log.warn("SerializationException: " + e.message)
                                throw BadRequestException(e.message)
                            } catch (e: IOException) {
                                log.warn("IOException: ${datapoolUrl} " + e.message)
                                throw BadRequestException(e.message)
                            }
                        } else {
                            throw BadRequestException("No DID given!")
                        }
                    }
                }
            }
        }
    }

}

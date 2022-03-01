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
import io.ktor.request.*
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
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.client.*

import com.auth0.jwt.*

import java.io.File
import java.io.IOException
import java.time.LocalDateTime

import kotlin.reflect.typeOf
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

import io.bkbn.kompendium.auth.configuration.JwtAuthConfiguration
import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.core.metadata.ExceptionInfo
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.DeleteInfo
import io.bkbn.kompendium.core.metadata.method.GetInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.Notarized.notarizedDelete
import io.bkbn.kompendium.core.routes.redoc

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.exceptions.ExposedSQLException

import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.entities.*

suspend fun retrieveBusinessPartnerInfo(datapoolUrl: String, bpn: String, token: String): String {

    var stringBody: String = ""
    HttpClient(Apache).use { client ->
        val httpResponse: HttpResponse = client.get("${datapoolUrl}/api/catena/business-partner/${bpn}") {
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
                    p { a("/docs") { +"See API docs" }}
                    p { a("/ui/") { +"Admin UI" }}
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

                route("/companies") {
                    handle {

                        val userSession = call.sessions.get<UserSession>()
                        if (userSession != null) {
                            // could be used later
                        }

                        call.respond(CompanyDao.getAll())
                    }
                }

                route("/companies/{did}/full") {
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
                                val d = Json { ignoreUnknownKeys = true }.decodeFromString<BusinessPartnerInfo>(BusinessPartnerInfo.serializer(), stringBody)
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
                                throw BadRequestException(e.message!!)
                            } catch (e: IOException) {
                                log.warn("IOException: ${datapoolUrl} " + e.message)
                                throw BadRequestException(e.message!!)
                            }
                        } else {
                            throw BadRequestException("No DID given!")
                        }
                    }
                }

            }
        }

        // this is just a workaround for the serialization issue
        @Serializable(with = CompanyCreateListSerializer::class) 
        class CompanyCreateDtoList : ArrayList<CompanyCreateDto> {
            constructor(l: List<CompanyCreateDto>) : super(l)
        }

        route("/api/company") {

            val authConfig = object : JwtAuthConfiguration {
                override val name: String = "auth-jwt"
            }

            // based on: authenticate("auth-jwt")
            notarizedAuthenticate(authConfig) {

                notarizedGet(GetInfo<Unit, List<CompanyCreateDto>>(
                    summary = "List of companies",
                    description = "Retrieve list of registered companies with wallets",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "List of companies is available",
                        // somehow the serialization did not work out with directly specifying `listOf(CompanyCreateDto(...`
                        examples = mapOf("demo" to CompanyCreateDtoList(listOf(CompanyCreateDto("did1_bpn", "did1_name", WalletCreateDto("did1", emptyList<String>())))))
                    ),
                    tags = setOf("Company (TO BE REVISED)")
                )) {
                    call.respond(CompanyDao.getAll())
                }

                // for documentation
                val notFoundException = ExceptionInfo<ExceptionResponse>(
                    responseType = typeOf<ExceptionResponse>(),
                    description = "No company with this bpn",
                    status = HttpStatusCode.NotFound,
                    examples = mapOf("demo" to ExceptionResponse("No company with this bpn"))
                )

                route("/{bpn}") {

                    // for documentation
                    val badRequestException = ExceptionInfo<ExceptionResponse>(
                        responseType = typeOf<ExceptionResponse>(),
                        description = "Missing or malformed bpn",
                        status = HttpStatusCode.BadRequest,
                        examples = mapOf("demo" to ExceptionResponse("Missing or malformed bpn"))
                    )

                    notarizedGet(GetInfo<Unit, CompanyDto>(
                        summary = "Retrieve single company by bpn",
                        description = "Retrieve single registered company with wallet by bpn",
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.OK,
                            description = "Company with the given bpn exists",
                            examples = mapOf("did1_bpn" to CompanyDto("did1_bpn", "did1_name", WalletDto("did1", LocalDateTime.now(), "samplePublicKey", emptyList<String>())))
                        ),
                        canThrow = setOf(badRequestException, notFoundException),
                        tags = setOf("Company (TO BE REVISED)")
                    )) {
                        val bpn = call.parameters["bpn"] ?: throw BadRequestException("Missing or malformed bpn")
                        val company = CompanyDao.getCompany(bpn)
                        val wallet = WalletDao.getWalletForCompany(company)
                        call.respond(CompanyDao.toObject(company, wallet))
                    }
                }

                // for documentation
                val illegalArgumentException = ExceptionInfo<ExceptionResponse>(
                    responseType = typeOf<ExceptionResponse>(),
                    description = "Illegal argument",
                    status = HttpStatusCode.BadRequest,
                    examples = mapOf("demo" to ExceptionResponse("Illegal argument"))
                )

                notarizedPost(PostInfo<Unit, CompanyCreateDto, SuccessResponse>(
                    summary = "Register company with wallet",
                    description = "Register a company with a hosted wallet",
                    requestInfo = RequestInfo(
                        description = "Company and wallet to register",
                        examples = mapOf("demo" to CompanyCreateDto("did1_bpn", "did1_name", WalletCreateDto("did1", emptyList<String>())))
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "Company and wallet was successfully registered",
                        examples = mapOf("demo" to SuccessResponse("Company and wallet successfully registered!"))
                    ),
                    canThrow = setOf(illegalArgumentException),
                    tags = setOf("Company (TO BE REVISED)")
                )) {

                    try {
                        val company = call.receive<CompanyCreateDto>()

                        transaction {
                            val c = Company.new {
                                bpn = company.bpn
                                name = company.name
                            }
                            var w = company.wallet
                            if (WalletCreateDto.INVALID == company.wallet) {
                                w = WalletCreateDto("did:bpn:BPNL" + (Math.random().toRawBits()).toString(16) + "ZZ", emptyList<String>())
                            }
                            WalletDao.createWallet(c, w)
                            // TODO should we respond with the created company?
                        }

                        call.respond(HttpStatusCode.Created, SuccessResponse("Company and wallet successfully registered!"))
                    } catch (e: IllegalArgumentException) {
                        throw BadRequestException(e.message!!)
                    } catch (e: ExposedSQLException) {
                        val isUniqueConstraintError = e.sqlState == "23505"
                        if (isUniqueConstraintError) {
                            throw BadRequestException("Company with given bpn already exists!")
                        } else {
                            throw BadRequestException(e.message!!)
                        }
                    }
                }

                route("/{bpn}") {
                    notarizedDelete(DeleteInfo<Unit, SuccessResponse>(
                        summary = "Remove company and hosted wallet",
                        description = "Remove company and hosted wallet",
                        responseInfo = ResponseInfo(
                            status = HttpStatusCode.Accepted,
                            description = "Company and wallet successfully removed!",
                            examples = mapOf("demo" to SuccessResponse("Company and wallet successfully removed!"))
                        ),
                        canThrow = setOf(notFoundException),
                        tags = setOf("Company (TO BE REVISED)")
                    )) {
                        val bpn = call.parameters["bpn"] ?: return@notarizedDelete call.respond(HttpStatusCode.BadRequest)
                        transaction {
                            val company = CompanyDao.getCompany(bpn)
                            WalletDao.getWalletForCompany(company).delete()
                            company.delete()
                        }
                        call.respond(HttpStatusCode.Accepted, SuccessResponse("Company and wallet successfully removed!"))
                    }
                }

            }

        }

    }

}

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

import kotlin.reflect.typeOf
import kotlinx.html.*
import kotlinx.serialization.Serializable

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

fun Application.configureRouting() {

    routing {

        redoc(pageTitle = "Simple API Docs")

        get("/") {
            call.respondHtml {
                head {
                    title { +"Catena-X Core // Custodian" }
                    style { +"body { font-family: monospace; }" }
                }
                body {
                    val runtime = Runtime.getRuntime()
                    h1 { +"Catena-X Core // Custodian" }
                    p { +"Below a few runtime statistics" }
                    ul {
                        li { +"Runtime.getRuntime().availableProcessors(): ${runtime.availableProcessors()}" }
                        li { +"Runtime.getRuntime().freeMemory(): ${runtime.freeMemory()}" }
                        li { +"Runtime.getRuntime().totalMemory(): ${runtime.totalMemory()}" }
                        li { +"Runtime.getRuntime().maxMemory(): ${runtime.maxMemory()}" }
                        li { +"System.getProperty(\"user.name\"): ${System.getProperty("user.name")}" }
                    }
                    p { a("/docs") { +"See API docs" }}
                }
            }
        }

        route("/company") {

            notarizedGet(GetInfo<Unit, List<CompanyDto>>(
                summary = "List of companies",
                description = "Retrieve list of registered companies with wallets",
                responseInfo = ResponseInfo(
                    status = HttpStatusCode.OK,
                    description = "List of companies is available",
                    examples = mapOf("demo" to mutableListOf(CompanyDto("did1_bpn", "did1_name", WalletDto("did1", emptyList<String>()))))
                ),
                tags = setOf("Company")
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
                        examples = mapOf("did1_bpn" to CompanyDto("did1_bpn", "did1_name", WalletDto("did1", emptyList<String>())))
                    ),
                    canThrow = setOf(badRequestException, notFoundException),
                    tags = setOf("Company")
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

            notarizedPost(PostInfo<Unit, CompanyDto, SuccessResponse>(
                summary = "Register company with wallet",
                description = "Register a company with a hosted wallet",
                requestInfo = RequestInfo(
                    description = "Company and wallet to register",
                    examples = mapOf("demo" to CompanyDto("did1_bpn", "did1_name", WalletDto("did1", emptyList<String>())))
                ),
                responseInfo = ResponseInfo(
                    status = HttpStatusCode.Created,
                    description = "Company and wallet was successfully registered",
                    examples = mapOf("demo" to SuccessResponse("Company and wallet successfully registered!"))
                ),
                canThrow = setOf(illegalArgumentException),
                tags = setOf("Company")
            )) {

                try {
                    val company = call.receive<CompanyDto>()

                    transaction {
                        val c = Company.new {
                            bpn = company.bpn
                            name = company.name
                        }
                        val w = WalletDao.createWallet(c, company.wallet)
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
                    summary = "Remove compand and hosted wallet",
                    description = "Remove compand and hosted wallet",
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Accepted,
                        description = "Company and wallet successfully removed!",
                        examples = mapOf("demo" to SuccessResponse("Company and wallet successfully removed!"))
                    ),
                    canThrow = setOf(notFoundException),
                    tags = setOf("Company")
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

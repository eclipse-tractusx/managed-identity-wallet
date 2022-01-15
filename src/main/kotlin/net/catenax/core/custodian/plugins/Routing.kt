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
import kotlinx.html.*

import io.bkbn.kompendium.swagger.swaggerUI

import net.catenax.core.custodian.models.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondHtml {
                head {
                    title { +"Catena-X Core // Custodian" }
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
                }
            }
        }

        route("/company") {
            get {
                call.respond(companyStorage)
            }

            get("{bpn}") {
                val bpn = call.parameters["bpn"] ?: return@get call.respondText(
                    "Missing or malformed id",
                    status = HttpStatusCode.BadRequest
                )
                val company =
                    companyStorage.find { it.bpn == bpn } ?: return@get call.respondText(
                        "No company with bpn $bpn",
                        status = HttpStatusCode.NotFound
                    )
                call.respond(company)
            }
            post {
                val company = call.receive<Company>()
                // TODO - This shouldn't really be done in production as
                // we should be accessing a mutable list in a thread-safe manner.
                // However, in production code we wouldn't be using mutable lists as a database!
                companyStorage.add(company)
                call.respondText("Company stored correctly", status = HttpStatusCode.Created)
            }
            delete("{bpn}") {
                val bpn = call.parameters["bpn"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (companyStorage.removeIf { it.bpn == bpn }) {
                    call.respondText("Company removed correctly", status = HttpStatusCode.Accepted)
                } else {
                    call.respondText("Not Found", status = HttpStatusCode.NotFound)
                }
            }
        }

        // This is all you need to do to add Swagger! Reachable at `/swagger-ui`
        swaggerUI()

    }

}

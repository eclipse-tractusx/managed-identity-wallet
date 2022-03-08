package net.catenax.core.custodian

// for 2.0.0-beta
// import io.ktor.server.engine.*
// import io.ktor.server.application.*

// for 1.6.7
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

import net.catenax.core.custodian.plugins.*
import net.catenax.core.custodian.models.ExceptionResponse
import net.catenax.core.custodian.models.NotFoundException
import net.catenax.core.custodian.models.BadRequestException
import net.catenax.core.custodian.models.ConflictException
import net.catenax.core.custodian.routes.appRoutes

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureSockets()
    configureSerialization()

    install(DefaultHeaders)
    
    // for debugging
    install(CallLogging)

    // Installs the Kompendium Plugin and sets up baseline server metadata
    configureOpenAPI()

    install(StatusPages) {
        exception<BadRequestException> { cause ->
            call.respond(HttpStatusCode.BadRequest, ExceptionResponse(cause.message!!))
        }
        exception<NotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, ExceptionResponse(cause.message!!))
        }
        exception<ConflictException> { cause ->
            call.respond(HttpStatusCode.Conflict, ExceptionResponse(cause.message!!))
        }
    }

    configureSecurity()

    configureRouting()
    appRoutes()

    configurePersistence()
}

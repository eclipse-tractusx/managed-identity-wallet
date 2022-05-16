package net.catenax.core.managedidentitywallets

// for 2.0.0-beta
// import io.ktor.server.engine.*
// import io.ktor.server.application.*

// for 1.6.7
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import net.catenax.core.managedidentitywallets.models.*
import net.catenax.core.managedidentitywallets.models.BadRequestException
import net.catenax.core.managedidentitywallets.models.NotFoundException
import net.catenax.core.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import net.catenax.core.managedidentitywallets.persistence.repositories.CredentialRepository
import net.catenax.core.managedidentitywallets.persistence.repositories.WalletRepository

import net.catenax.core.managedidentitywallets.plugins.*
import net.catenax.core.managedidentitywallets.routes.appRoutes
import net.catenax.core.managedidentitywallets.services.BusinessPartnerDataService
import net.catenax.core.managedidentitywallets.services.BusinessPartnerDataServiceImpl
import net.catenax.core.managedidentitywallets.services.WalletService

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
        exception<UnprocessableEntityException> { cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, ExceptionResponse(cause.message!!))
        }
        exception<ForbiddenException> { cause ->
            call.respond(HttpStatusCode.Forbidden, ExceptionResponse(cause.message!!))
        }
        exception<NotImplementedException> { cause ->
            call.respond(HttpStatusCode.NotImplemented, ExceptionResponse(cause.message!!))
        }
        exception<NotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, ExceptionResponse(cause.message!!))
        }
        exception<ConflictException> { cause ->
            call.respond(HttpStatusCode.Conflict, ExceptionResponse(cause.message!!))
        }
    }

    configureSecurity()

    val walletRepository = WalletRepository()
    val credRepository = CredentialRepository()
    val acaPyConfig = WalletAndAcaPyConfig(
        apiAdminUrl = environment.config.property("acapy.apiAdminUrl").getString(),
        networkIdentifier = environment.config.property("acapy.networkIdentifier").getString(),
        adminApiKey = environment.config.property("acapy.adminApiKey").getString(),
        catenaXBpn = environment.config.property("wallet.catenaXBpn").getString()
    )
    val walletService = WalletService.createWithAcaPyService(acaPyConfig, walletRepository, credRepository)
    val businessPartnerDataService = BusinessPartnerDataService.createBusinessPartnerDataService(walletService)
    configureRouting(walletService)
    appRoutes(walletService, businessPartnerDataService)

    configurePersistence()
}

package net.catenax.core.custodian.routes

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.auth.configuration.JwtAuthConfiguration
import io.ktor.application.*
import io.ktor.routing.*
import net.catenax.core.custodian.persistence.repositories.CredentialRepository
import net.catenax.core.custodian.persistence.repositories.WalletRepository
import net.catenax.core.custodian.services.WalletService

fun Application.appRoutes() {
    val walletRepository = WalletRepository()
    val credRepository = CredentialRepository()
    val walletService = WalletService(walletRepository, credRepository)

    routing {
        route("/api") {

            val authConfig = object : JwtAuthConfiguration {
                override val name: String = "auth-jwt"
            }

            // based on: authenticate("auth-jwt")
            notarizedAuthenticate(authConfig) {
                walletRoutes(walletService)
                businessPartnerDataRoutes()
                didDocRoutes(walletService)
                vcRoutes(walletService)
                vpRoutes(walletService)
            }
        }
    }
}

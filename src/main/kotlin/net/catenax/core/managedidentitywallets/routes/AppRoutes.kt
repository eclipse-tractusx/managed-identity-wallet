package net.catenax.core.managedidentitywallets.routes

import io.ktor.application.*
import io.ktor.routing.*
import net.catenax.core.managedidentitywallets.services.BusinessPartnerDataService
import net.catenax.core.managedidentitywallets.services.WalletService

fun Application.appRoutes(walletService: WalletService, businessPartnerDataService: BusinessPartnerDataService) {

    routing {
        route("/api") {

            walletRoutes(walletService, businessPartnerDataService)
            businessPartnerDataRoutes(businessPartnerDataService)
            didDocRoutes(walletService)
            vcRoutes(walletService)
            vpRoutes(walletService)

        }
    }
}

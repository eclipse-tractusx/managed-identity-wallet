package net.catenax.core.managedidentitywallets.plugins

import io.ktor.application.*

import io.bkbn.kompendium.core.Kompendium
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.bkbn.kompendium.oas.server.Server

import java.net.URI

fun Application.configureOpenAPI() {
    val version = environment.config.property("app.version").getString()
    install(Kompendium) {
      spec = OpenApiSpec(
          openapi = "3.0.3",
          info = Info(
          title = "Catena-X Core Managed Identity Wallets API",
          version = version,
          description = "Catena-X Core Managed Identity Wallets API",
          // TODO need to be adjusted
          termsOfService = URI("https://www.catena-x.net/"),
          contact = Contact(
              name = "Catena-X Core Agile Release Train",
              email = "info@catena-x.net",
              url = URI("https://www.catena-x.net/")
          ),
          license = License(
              name = "Apache 2.0",
              url = URI("https://github.com/catenax-ng/product-core-managed-identity-wallets/blob/develop/LICENSE")
          )
          ),
          servers = mutableListOf(
          Server(
              url = URI("http://localhost:8080"),
              description = "Local Dev Environment"
          ),
          Server(
              url = URI("https://managed-identity-wallets.dev.demo.catena-x.net"),
              description = "Catena-X Dev Environment"
          ),
          Server(
              url = URI("https://managed-identity-wallets.int.demo.catena-x.net"),
              description = "Catena-X Int Environment"
          )
          )
      )
    }
}

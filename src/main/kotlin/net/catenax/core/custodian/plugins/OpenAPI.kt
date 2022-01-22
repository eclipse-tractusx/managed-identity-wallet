package net.catenax.core.custodian.plugins

import io.ktor.application.*

import io.bkbn.kompendium.core.Kompendium
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.bkbn.kompendium.oas.server.Server

import java.net.URI

fun Application.configureOpenAPI() {
    install(Kompendium) {
      spec = OpenApiSpec(
          openapi = "3.0.3",
          info = Info(
          title = "Catena-X Core Custodian API",
          version = "0.0.1",
          description = "Catena-X Core Custodian API",
          // TODO need to be adjusted
          termsOfService = URI("https://www.catena-x.net/"),
          contact = Contact(
              name = "Catena-X Core Agile Release Train",
              email = "info@catena-x.net",
              url = URI("https://www.catena-x.net/")
          ),
          license = License(
              name = "Apache 2.0",
              url = URI("https://github.com/catenax/core-custodian/blob/main/LICENSE")
          )
          ),
          servers = mutableListOf(
          Server(
              url = URI("https://int.catena-x.net"),
              description = "Catena-X Integration Environment"
          )
          )
      )
    }
}

package net.catenax.core.custodian

// for 2.0.0-beta
// import io.ktor.server.engine.*
// import io.ktor.server.application.*

import io.ktor.server.netty.*

// for 1.6.7
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.webjars.Webjars
import io.ktor.features.*

import io.bkbn.kompendium.core.Kompendium
import io.bkbn.kompendium.core.Notarized.notarizedGet
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.bkbn.kompendium.oas.server.Server

import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi

import java.net.URI

import net.catenax.core.custodian.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSockets()
        configureSerialization()
        configureRouting()

        install(DefaultHeaders)
        
        // for debugging
        install(CallLogging)

        // Installs the Kompendium Plugin and sets up baseline server metadata
        install(Webjars)
        install(Kompendium) {
            spec = OpenApiSpec(
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

    }.start(wait = true)
}

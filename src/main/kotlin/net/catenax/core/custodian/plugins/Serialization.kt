package net.catenax.core.custodian.plugins

// for 2.0.0-beta
// import io.ktor.serialization.kotlinx.json.*
// import io.ktor.server.plugins.*
// import io.ktor.server.application.*
// import io.ktor.server.response.*
// import io.ktor.server.request.*
// import io.ktor.server.routing.*

// for 1.6.7
import io.ktor.serialization.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

import net.catenax.core.custodian.models.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }
}

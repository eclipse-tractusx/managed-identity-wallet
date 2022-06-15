package net.catenax.core.managedidentitywallets.plugins

// for 1.6.7
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*

import java.time.*

// for 2.0.0-beta
// import io.ktor.server.websocket.*
// import io.ktor.websocket.*
// import io.ktor.server.application.*
// import io.ktor.server.response.*
// import io.ktor.server.request.*
// import io.ktor.server.routing.*

import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/") { // websocketSession
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        outgoing.send(Frame.Text("YOU SAID: $text"))
                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            }
        }
    }
}

package net.catenax.core.custodian

import io.ktor.http.*

// for 2.0.0-beta
// import io.ktor.server.response.*
// import io.ktor.server.request.*
// import io.ktor.server.routing.*
// import io.ktor.server.websocket.*
// import io.ktor.websocket.*
// import io.ktor.serialization.kotlinx.json.*
// import io.ktor.server.plugins.*
// import io.ktor.server.application.*

// for 1.6.7
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.application.*

import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.config.*

import io.ktor.auth.*

import net.catenax.core.custodian.plugins.*

class ApplicationTest {

    fun setupEnvironment(environment: ApplicationEnvironment) {
        (environment.config as MapApplicationConfig).apply {
            put("app.version", System.getenv("APP_VERSION") ?: "0.0.6")
            put("db.jdbcUrl", System.getenv("CX_DB_JDBC_URL") ?: "jdbc:h2:mem:custodian;DB_CLOSE_DELAY=-1;")
            put("db.jdbcDriver", System.getenv("CX_DB_JDBC_DRIVER") ?: "org.h2.Driver")
            put("auth.issuerUrl", System.getenv("CX_AUTH_ISSUER_URL") ?: "http://localhost:8081/auth/realms/catenax")
            put("auth.realm", System.getenv("CX_AUTH_REALM") ?: "catenax")
            put("auth.role", System.getenv("CX_AUTH_ROLE") ?: "access")
            put("datapool.url", System.getenv("CX_DATAPOOL_URL") ?: "http://0.0.0.0:8080")
        }
    }

   fun makeToken(): String = "token"

   fun Application.configureTestSecurity() {

        // dummy authentication for tests
        install(Authentication) {

            provider("auth-ui") {
                pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
                    context.principal(UserIdPrincipal("tester"))
                }
            }

            provider("auth-ui-session") {
                pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
                    context.principal(UserIdPrincipal("tester"))
                }
            }

            provider("auth-jwt") {
                pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
                    context.principal(UserIdPrincipal("tester"))
                }
            }

        }
    }

    @Test
    fun testRoot() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureTestSecurity()
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue(response.content!!.contains("Catena-X Core"))
            }
            handleRequest(HttpMethod.Post, "/").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/docs").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            handleRequest(HttpMethod.Get, "/openapi.json").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}

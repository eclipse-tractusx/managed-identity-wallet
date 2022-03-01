package net.catenax.core.custodian

import io.ktor.http.*
import java.time.Duration

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

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

import io.ktor.auth.*

import net.catenax.core.custodian.entities.*
import net.catenax.core.custodian.plugins.*
import net.catenax.core.custodian.models.*

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

    @Test
    fun testCompanyCrud() {
        val token = makeToken()

        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureTestSecurity()
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Get, "/api/company") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(0, companies.size)
            }
            // create it with wallet
            handleRequest(HttpMethod.Post, "/api/company") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn1", "name": "name1", "wallet": { "did": "did1", "vcs": [] }}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            // create it without wallet, on the fly
            handleRequest(HttpMethod.Post, "/api/company") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn2", "name": "name2"}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            handleRequest(HttpMethod.Get, "/api/company") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(2, companies.size)
            }

            // programmatically add a company
            transaction {
                val c = Company.new {
                    bpn = "bpn3"
                    name = "name3"
                }
                WalletDao.createWallet(c, WalletCreateDto("did3", emptyList<String>()))
            }

            handleRequest(HttpMethod.Get, "/api/company") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(3, companies.size)
            }
        }
    }
    @Test
    fun testCompanyCrudExceptions() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureTestSecurity()
            configureOpenAPI()
            configureRouting()
            configureSerialization()
        }) {
            var exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("{wrong:json}")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"wrong":"json"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": {}}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("did"))
            assertTrue(exception.message!!.contains("vcs"))
            var iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"", "name": "name1", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'bpn' is required not to be blank, but it was blank", iae.message)
            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'name' is required not to be blank, but it was blank", iae.message)
            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": { "did": "", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'did' is required not to be blank, but it was blank", iae.message)

            // programmatically add a company
            transaction {
                val c = Company.new {
                    bpn = "bpn4"
                    name = "name4"
                }
                WalletDao.createWallet(c, WalletCreateDto("did4", emptyList<String>()))
            }

            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn4", "name": "name4", "wallet": { "did": "did4", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Company with given bpn already exists!", iae.message)

        }
    }

}
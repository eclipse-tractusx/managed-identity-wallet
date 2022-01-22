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

import net.catenax.core.custodian.entities.*
import net.catenax.core.custodian.plugins.*
import net.catenax.core.custodian.models.*

class ApplicationTest {

   fun setupEnvironment(environment: ApplicationEnvironment) {
        (environment.config as MapApplicationConfig).apply {
            put("db.jdbcUrl", System.getenv("CX_DB_JDBC_URL") ?: "jdbc:h2:mem:custodian;DB_CLOSE_DELAY=-1;")
            put("db.jdbcDriver", System.getenv("CX_DB_JDBC_DRIVER") ?: "org.h2.Driver")
        }
   }

    @Test
    fun testRoot() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
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
        }
    }
    @Test
    fun testCompanyCrud() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Get, "/company") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(0, companies.size)
            }
            handleRequest(HttpMethod.Post, "/company") {
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("""{"bpn":"bpn1", "name": "name1", "wallet": { "did": "did1", "vcs": [] }}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            handleRequest(HttpMethod.Get, "/company") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(1, companies.size)
            }

            // programmatically add a company
            transaction {
                val c = Company.new {
                    bpn = "bpn2"
                    name = "name2"
                }
                WalletDao.createWallet(c, WalletDto("did2", emptyList<String>()))
            }

            handleRequest(HttpMethod.Get, "/company") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<CompanyDto> = Json.decodeFromString(ListSerializer(CompanyDto.serializer()), response.content!!)
                assertEquals(2, companies.size)
            }
        }
    }
    @Test
    fun testCompanyCrudExceptions() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureSerialization()
            configureOpenAPI()
            configureRouting()
        }) {
            var exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("{wrong:json}")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"wrong":"json"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": ""}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wallet"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
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
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"", "name": "name1", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'bpn' is required not to be blank, but it was blank", iae.message)
            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'name' is required not to be blank, but it was blank", iae.message)
            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
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
                    bpn = "bpn3"
                    name = "name3"
                }
                WalletDao.createWallet(c, WalletDto("did3", emptyList<String>()))
            }

            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn3", "name": "name3", "wallet": { "did": "did3", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Company with given bpn already exists!", iae.message)

        }
    }

}
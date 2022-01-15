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

import kotlin.test.*
import io.ktor.server.testing.*
import net.catenax.core.custodian.plugins.*
import net.catenax.core.custodian.models.*

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
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
            configureRouting()
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Get, "/company") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<Company> = Json.decodeFromString(ListSerializer(Company.serializer()), response.content!!)
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
                val companies: List<Company> = Json.decodeFromString(ListSerializer(Company.serializer()), response.content!!)
                assertEquals(1, companies.size)
            }

            // programmatically add a company
            companyStorage.add(Company("bpn2", "name2", Wallet("did2", emptyList<String>())))

            handleRequest(HttpMethod.Get, "/company") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val companies: List<Company> = Json.decodeFromString(ListSerializer(Company.serializer()), response.content!!)
                assertEquals(2, companies.size)
            }
        }
    }
    @Test
    fun testCompanyCrudExceptions() {
        withTestApplication({
            configureRouting()
            configureSerialization()
        }) {
            var exception = assertFailsWith<SerializationException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("{wrong:json}")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<SerializationException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"wrong":"json"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<SerializationException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<SerializationException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<SerializationException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": ""}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wallet"))
            exception = assertFailsWith<SerializationException> {
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
            var iae = assertFailsWith<IllegalArgumentException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"", "name": "name1", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("BPN is blank", iae.message)
            iae = assertFailsWith<IllegalArgumentException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": { "did": "did1", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Name is blank", iae.message)
            iae = assertFailsWith<IllegalArgumentException> {
                handleRequest(HttpMethod.Post, "/company") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn1", "name": "", "wallet": { "did": "", "vcs": [] }}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("DID is blank", iae.message)
        }
    }

}
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
import io.ktor.application.*
import io.ktor.server.testing.*
import io.ktor.config.*
import io.ktor.auth.*
import kotlinx.coroutines.runBlocking

import kotlin.test.*

import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*

import net.catenax.core.custodian.plugins.*
import net.catenax.core.custodian.models.*
import net.catenax.core.custodian.routes.*
import net.catenax.core.custodian.services.*
import net.catenax.core.custodian.persistence.repositories.*

import org.jetbrains.exposed.sql.transactions.transaction

class ApplicationTest {

     private fun setupEnvironment(environment: ApplicationEnvironment) {
        (environment.config as MapApplicationConfig).apply {
            put("app.version", System.getenv("APP_VERSION") ?: "0.0.7")
            put("db.jdbcUrl", System.getenv("CX_DB_JDBC_URL") ?: "jdbc:h2:mem:custodian;DB_CLOSE_DELAY=-1;")
            put("db.jdbcDriver", System.getenv("CX_DB_JDBC_DRIVER") ?: "org.h2.Driver")
            put("auth.issuerUrl", System.getenv("CX_AUTH_ISSUER_URL") ?: "http://localhost:8081/auth/realms/catenax")
            put("auth.realm", System.getenv("CX_AUTH_REALM") ?: "catenax")
            put("auth.role", System.getenv("CX_AUTH_ROLE") ?: "access")
            put("datapool.url", System.getenv("CX_DATAPOOL_URL") ?: "http://0.0.0.0:8080")
            put("acapy.apiAdminUrl", System.getenv("ACAPY_API_ADMIN_URL") ?: "http://localhost:11000")
            put("acapy.networkIdentifier", System.getenv("ACAPY_NETWORK_IDENTIFIER") ?: ":indy:test")
            put("wallet.catenaXBpn", System.getenv("CX_BPN") ?: "bpn1")
        }
    }

    private val walletRepository = WalletRepository()
    private val credentialRepository = CredentialRepository()
    private val acaPyMockedService = AcaPyMockedService()
    private val walletService = AcaPyWalletServiceImpl(acaPyMockedService, walletRepository, credentialRepository)

    private fun makeToken(): String = "token"

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
            configureRouting(walletService)
            appRoutes(walletService)
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
    fun testWalletCrud() {
        val token = makeToken()

        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureTestSecurity()
            configureRouting(walletService)
            appRoutes(walletService)
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(0, wallets.size)
            }
            // create wallet
            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn1", "name": "name1"}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(1, wallets.size)
            }

            // programmatically add a wallet
            transaction {
                runBlocking {
                    walletService.createWallet(WalletCreateDto("did3", "name3"))
                }
            }

            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(2, wallets.size)
            }

            // delete both from the store
            handleRequest(HttpMethod.Delete, "/api/wallets/bpn1").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            transaction {
                runBlocking {
                    walletService.deleteWallet("did3")
                }
            }

            // verify deletion
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(0, wallets.size)
            }

            transaction {
                assertEquals(0, walletService.getAll().size)
            }
        }
    }

    @Test
    fun testWalletCrudExceptions() {
        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureTestSecurity()
            configureOpenAPI()
            configureRouting(walletService)
            appRoutes(walletService)
            configureSerialization()
        }) {
            // create catena X wallet
            transaction {
                runBlocking {
                    walletService.createWallet(WalletCreateDto("bpn1", "name1"))
                }
            }

            var exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("wrong:json")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wrong"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"wrong":"json"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("required"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                       addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            exception = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                       addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                       addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn2", "name": null}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertTrue(exception.message!!.contains("null"))
            var iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"", "name": "name2"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'bpn' is required not to be blank, but it was blank", iae.message)
            iae = assertFailsWith<BadRequestException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn2", "name": ""}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Field 'name' is required not to be blank, but it was blank", iae.message)

            // programmatically add a wallet
            transaction {
                runBlocking {
                    walletService.createWallet(WalletCreateDto("bpn4", "name4"))
                }
            }

            var ce = assertFailsWith<ConflictException> {
                handleRequest(HttpMethod.Post, "/api/wallets") {
                        addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("""{"bpn":"bpn4", "name": "name4"}""")
                }.apply {
                    assertEquals(HttpStatusCode.Created, response.status())
                }
            }
            assertEquals("Wallet with identifier bpn4 already exists!", ce.message)

            // clean up created wallets
            transaction {
                runBlocking {
                    walletService.deleteWallet("bpn4")
                    walletService.deleteWallet("bpn1") // Catena-X wallet
                    assertEquals(0, walletService.getAll().size)
                }
            }
        }
    }

    @Test
    fun testDataUpdate() {
        val token = makeToken()

        withTestApplication({
            setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureTestSecurity()
            configureRouting(walletService)
            appRoutes(walletService)
            configureSerialization()
        }) {
            handleRequest(HttpMethod.Post, "/api/businessPartnerData") {
                addHeader(HttpHeaders.Authorization, "Bearer $token")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
"""{
  "bpn": "BPNL000000000001",
  "identifiers": [
    {
      "uuid": "089e828d-01ed-4d3e-ab1e-cccca26814b3",
      "value": "BPNL000000000001",
      "type": {
        "technicalKey": "BPN",
        "name": "Business Partner Number",
        "url": ""
      },
      "issuingBody": {
        "technicalKey": "CATENAX",
        "name": "Catena-X",
        "url": ""
      },
      "status": {
        "technicalKey": "UNKNOWN",
        "name": "Unknown"
      }
    }
  ],
  "names": [
    {
      "uuid": "de3f3db6-e337-436b-a4e0-fc7d17e8af89",
      "value": "German Car Company",
      "shortName": "GCC",
      "type": {
        "technicalKey": "REGISTERED",
        "name": "The main name under which a business is officially registered in a country's business register.",
        "url": ""
      },
      "language": {
        "technicalKey": "undefined",
        "name": "Undefined"
      }
    },
    {
      "uuid": "defc3da4-92ef-44d9-9aee-dcedc2d72e0e",
      "value": "German Car Company",
      "shortName": "GCC",
      "type": {
        "technicalKey": "INTERNATIONAL",
        "name": "The international version of the local name of a business partner",
        "url": ""
      },
      "language": {
        "technicalKey": "undefined",
        "name": "Undefined"
      }
    }
  ],
  "legalForm": {
    "technicalKey": "DE_AG",
    "name": "Aktiengesellschaft",
    "url": "",
    "mainAbbreviation": "AG",
    "language": {
      "technicalKey": "de",
      "name": "German"
    },
    "categories": [
      {
        "name": "AG",
        "url": ""
      }
    ]
  },
  "status": null,
  "addresses": [
    {
      "uuid": "16701107-9559-4fdf-b1c1-8c98799d779d",
      "version": {
        "characterSet": {
          "technicalKey": "WESTERN_LATIN_STANDARD",
          "name": "Western Latin Standard (ISO 8859-1; Latin-1)"
        },
        "language": {
          "technicalKey": "en",
          "name": "English"
        }
      },
      "careOf": null,
      "contexts": [],
      "country": {
        "technicalKey": "DE",
        "name": "Germany"
      },
      "administrativeAreas": [
        {
          "uuid": "cc6de665-f8eb-45ed-b2bd-6caa28fa8368",
          "value": "Bavaria",
          "shortName": "BY",
          "fipsCode": "GM02",
          "type": {
            "technicalKey": "REGION",
            "name": "Region",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "postCodes": [
        {
          "uuid": "8a02b3d0-de1e-49a5-9528-cfde2d5273ed",
          "value": "80807",
          "type": {
            "technicalKey": "REGULAR",
            "name": "Regular",
            "url": ""
          }
        }
      ],
      "localities": [
        {
          "uuid": "2cd18685-fac9-49f4-a63b-322b28f7dc9a",
          "value": "Munich",
          "shortName": "M",
          "type": {
            "technicalKey": "CITY",
            "name": "City",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "thoroughfares": [
        {
          "uuid": "0c491424-b2bc-44cf-9d14-71cbe513423f",
          "value": "Muenchner Straße 34",
          "name": "Muenchner Straße",
          "shortName": null,
          "number": "34",
          "direction": null,
          "type": {
            "technicalKey": "STREET",
            "name": "Street",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "premises": [],
      "postalDeliveryPoints": [],
      "geographicCoordinates": null,
      "types": [
        {
          "technicalKey": "HEADQUARTER",
          "name": "Headquarter",
          "url": ""
        }
      ]
    }
  ],
  "profileClassifications": [],
  "types": [
    {
      "technicalKey": "LEGAL_ENTITY",
      "name": "Legal Entity",
      "url": ""
    }
  ],
  "bankAccounts": [],
  "roles": [],
  "relations": []
}"""
                )
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }
        }
    }
}

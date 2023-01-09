/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.NotImplementedException
import org.eclipse.tractusx.managedidentitywallets.models.WalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import kotlin.test.*

@kotlinx.serialization.ExperimentalSerializationApi
class DidDocTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        SingletonTestData.cleanSingletonTestData()
        server.stop(1000, 10000)
    }

    @Test
    fun testDidDocumentOperations() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,
                EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.webhookService,
                EnvironmentTestSetup.utilsService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {
            // programmatically add a wallet
            runBlocking {
                EnvironmentTestSetup.walletService.initCatenaXWalletAndSubscribeForAriesWS(
                    bpn = EnvironmentTestSetup.DEFAULT_BPN,
                    did = EnvironmentTestSetup.DEFAULT_DID,
                    verkey = EnvironmentTestSetup.DEFAULT_VERKEY,
                    name = "Catena_X_Wallet"
                )
                EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.EXTRA_TEST_BPN, "name_extra")
                )
            }

            handleRequest(HttpMethod.Get, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Get, "/api/didDocuments/${EnvironmentTestSetup.EXTRA_TEST_BPN}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            val notValidDidMethod = "did:local:test:test:XMcRfSUkkQK38p6CCjHZz6"
            handleRequest(HttpMethod.Get, "/api/didDocuments/$notValidDidMethod") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            val notValidDidMethodShortId = "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}XMcRfSU"
            handleRequest(HttpMethod.Get, "/api/didDocuments/$notValidDidMethodShortId") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            val notSupportedDidMethod = "did:wrong:network:XMcRfSUkkQK38p6CCjHZz6"
            handleRequest(HttpMethod.Get, "/api/didDocuments/$notSupportedDidMethod") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            // Add new service endpoint `linked_domains`
            handleRequest(HttpMethod.Post, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{ "id": "linked_domains", "type": "linked_domains", "serviceEndpoint": "https://myhost:123"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            // Add new service endpoint `did-communication` that already exists
            handleRequest(HttpMethod.Post, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{ "id": "did-communication", "type": "did-communication", "serviceEndpoint": "https://myhost:123"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }

            // Add new service endpoint `profile`
            handleRequest(HttpMethod.Post, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{ "id": "profile", "type": "profile", "serviceEndpoint": "https://myhost:123"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            // Add invalid service endpoint `unknown-test`
            handleRequest(HttpMethod.Post, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{ "id": "unknown-test", "type": "unknown-test", "serviceEndpoint": "https://myhost:123"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.NotImplemented, response.status())
            }

            // Add service endpoint `linked_domains` for managed wallet
            handleRequest(HttpMethod.Post, "/api/didDocuments/${EnvironmentTestSetup.EXTRA_TEST_BPN}/services") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{ "id": "linked_domains", "type": "linked_domains", "serviceEndpoint": "https://myhost:123"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            // Delete service endpoint `linked_domains`
            handleRequest(HttpMethod.Delete, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services/linked_domains") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.NotImplemented, response.status())
            }

            // Update service endpoint `did-communication`
            handleRequest(HttpMethod.Put, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services/did-communication") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{"type": "did-communication","serviceEndpoint": "https://myhost:7712"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            // Update service endpoint `did-communication` for managed wallet
            handleRequest(HttpMethod.Put, "/api/didDocuments/${EnvironmentTestSetup.EXTRA_TEST_BPN}/services/did-communication") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{"type": "did-communication","serviceEndpoint": "https://myhost:7712"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            // Update service endpoint linked_domains that does not exists
            handleRequest(HttpMethod.Put, "/api/didDocuments/${EnvironmentTestSetup.DEFAULT_BPN}/services/linked_domains") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """{"type": "linked_domains","serviceEndpoint": "https://myhost:7712"}""".trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            // Update service endpoint linked_domains that does not exists
            val exception = assertFailsWith<NotFoundException> {
                runBlocking {
                    EnvironmentTestSetup.walletService.updateService(
                        EnvironmentTestSetup.EXTRA_TEST_BPN,
                        "linked_domains",
                        DidServiceUpdateRequestDto(
                            type = "linked_domains",
                            serviceEndpoint = "https://test123.com"
                        )
                    )
                }
            }
            assertTrue { exception.message!!.contains("Target Service Endpoint not Found") }

            SingletonTestData.didDocWithoutService = true
            // Update service endpoint linked_domains that does not exists
            val exceptionUpdateService = assertFailsWith<BadRequestException> {
                runBlocking {
                    EnvironmentTestSetup.walletService.updateService(
                        EnvironmentTestSetup.DEFAULT_BPN,
                        "linked_domains",
                        DidServiceUpdateRequestDto(
                            type = "linked_domains",
                            serviceEndpoint = "https://test123.com"
                        )
                    )
                }
            }
            assertTrue { exceptionUpdateService.message!!.contains("Update Service failed: DID Document has no services") }

            val notImplException = assertFailsWith<NotImplementedException> {
                Services.utilsService.mapServiceTypeToEnum("UnknownType")
            }
            assertTrue { notImplException.message!!.contains("Service type UnknownType is not supported") }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.EXTRA_TEST_BPN)
            }
        }
    }

}

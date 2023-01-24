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
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.Rfc23State
import org.eclipse.tractusx.managedidentitywallets.plugins.configureOpenAPI
import org.eclipse.tractusx.managedidentitywallets.plugins.configurePersistence
import org.eclipse.tractusx.managedidentitywallets.plugins.configureRouting
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSecurity
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSerialization
import org.eclipse.tractusx.managedidentitywallets.plugins.configureStatusPages
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@kotlinx.serialization.ExperimentalSerializationApi
class SelfManagedInteractionTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun cleanSingletonTestData() {
        SingletonTestData.cleanSingletonTestData()
    }

    @AfterTest
    fun tearDown() {
        server.stop(1000, 10000)
    }

    @Test
    fun testRegisterSelfManagedWalletAndTriggerIssuanceFlow() {
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
                EnvironmentTestSetup.walletService.initBaseWalletAndSubscribeForAriesWS(
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Catena-X"
                )

                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
                SingletonTestData.connectionId = "123"
                SingletonTestData.threadId = "456"
            }

            val selfManagedWalletCreateDto = SelfManagedWalletCreateDto(
                bpn = "e-bpn",
                did = "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}YHXZLLSLnKxz5D2HQaKXcP",
                name = "e-name",
                webhookUrl = "http://example.com/webhook"
            )
            handleRequest(HttpMethod.Post, "/api/wallets/self-managed-wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(SelfManagedWalletCreateDto.serializer(), selfManagedWalletCreateDto))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            runBlocking {
                transaction {
                    val storedSelfManagedWallet = EnvironmentTestSetup.walletService.getWallet(
                        selfManagedWalletCreateDto.did
                    )
                    assertEquals(true, storedSelfManagedWallet.pendingMembershipIssuance)

                    val connections = EnvironmentTestSetup.connectionRepository
                        .getConnections(SingletonTestData.baseWalletDID, null)
                    assertEquals(1, connections.size)
                    assertEquals(Rfc23State.REQUEST_SENT.toString(), connections[0].state)

                    val webhook = EnvironmentTestSetup.webhookService.getWebhookByThreadId(SingletonTestData.threadId)
                    assertNotNull(webhook)
                    assertEquals(Rfc23State.REQUEST_SENT.toString(), webhook.state)

                    EnvironmentTestSetup.webhookRepository.deleteWebhook(SingletonTestData.threadId)
                    val webhookConnection = EnvironmentTestSetup.webhookService.getWebhookByThreadId(SingletonTestData.threadId)
                    assertEquals(null, webhookConnection)

                    val listOfBpn = EnvironmentTestSetup.walletService.getAllBpns()
                    assertEquals(2, listOfBpn.size)
                }
            }

            // Issuance flow
            val vc = VerifiableCredentialIssuanceFlowRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2299-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                holderIdentifier = selfManagedWalletCreateDto.did,
                isRevocable = true,
                webhookUrl = "http://example.com/webhook"
            )

            SingletonTestData.threadId = "987"
            handleRequest(HttpMethod.Post, "/api/credentials/issuance-flow") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialIssuanceFlowRequestDto.serializer(),
                        vc,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertTrue(response.content!!.contains("Invalid connection between"))

            }

            // force update connection to `COMPLETED`
            runBlocking {
                transaction {
                    EnvironmentTestSetup.connectionRepository.updateConnectionState(
                        SingletonTestData.connectionId,
                        Rfc23State.COMPLETED.toString()
                    )
                }
            }

            handleRequest(HttpMethod.Post, "/api/credentials/issuance-flow") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(VerifiableCredentialIssuanceFlowRequestDto.serializer(), vc)
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // remove Webhook element to clean and avoid conflict
            runBlocking {
                transaction {
                    EnvironmentTestSetup.webhookRepository.deleteWebhook(SingletonTestData.threadId)
                    val webhookCredential = EnvironmentTestSetup.webhookService.getWebhookByThreadId(SingletonTestData.threadId)
                    assertEquals(null, webhookCredential)
                }
            }

            val vcWithCredentialSubjectId = VerifiableCredentialIssuanceFlowRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2299-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University", "id" to selfManagedWalletCreateDto.did),
                isRevocable = true,
                webhookUrl = "http://example.com/webhook"
            )

            handleRequest(HttpMethod.Post, "/api/credentials/issuance-flow") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialIssuanceFlowRequestDto.serializer(),
                        vcWithCredentialSubjectId,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            runBlocking {
                transaction {
                    val webhookCredential =
                        EnvironmentTestSetup.webhookService.getWebhookByThreadId(SingletonTestData.threadId)
                    assertNotNull(webhookCredential)
                    assertEquals(vc.webhookUrl, webhookCredential.webhookUrl)
                }
            }

            // clean
            runBlocking {
                transaction {
                    EnvironmentTestSetup.webhookRepository.deleteWebhook(SingletonTestData.threadId)
                    val webhookCredential = EnvironmentTestSetup.webhookService.getWebhookByThreadId(SingletonTestData.threadId)
                    assertEquals(null, webhookCredential)
                }

                var connections = EnvironmentTestSetup.connectionRepository.getAll()
                assertEquals(1, connections.size)

                EnvironmentTestSetup.walletService.deleteWallet("e-bpn")
                connections = EnvironmentTestSetup.connectionRepository.getAll()
                assertEquals(0, connections.size)

                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                val listOfBpn = EnvironmentTestSetup.walletService.getAllBpns()
                assertEquals(0, listOfBpn.size)
            }

        }
    }


}
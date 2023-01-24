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

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.Rfc23State
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.plugins.configureOpenAPI
import org.eclipse.tractusx.managedidentitywallets.plugins.configurePersistence
import org.eclipse.tractusx.managedidentitywallets.plugins.configureRouting
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSecurity
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSerialization
import org.eclipse.tractusx.managedidentitywallets.plugins.configureStatusPages
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import org.eclipse.tractusx.managedidentitywallets.services.AcaPyWalletServiceImpl
import org.eclipse.tractusx.managedidentitywallets.services.IAcaPyService
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.ManagedWalletsAriesEventHandler
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@kotlinx.serialization.ExperimentalSerializationApi
class ManagedWalletAriesEventHandlerTest {

    private val issuerWallet = WalletExtendedData(
        id = 1,
        name = "Base_Wallet",
        bpn = EnvironmentTestSetup.DEFAULT_BPN,
        did = EnvironmentTestSetup.DEFAULT_DID,
        walletId = null,
        walletKey = null,
        walletToken = null,
        revocationListName = null,
        pendingMembershipIssuance = false
    )

    private val connectionId = "connection-id"
    private val credentialId = "urn:uuid:93731387-dec1-4bf6-2227-31710f977177"

    private val holderWallet = WalletExtendedData(
        id = 2,
        name = "target wallet",
        bpn = "BPNLholder",
        did = "did:sov:holder1",
        walletId = "walletId_holder1",
        walletKey = "walletKey_holder1",
        walletToken = "walletToken_holder1",
        revocationListName = null,
        pendingMembershipIssuance = false
    )

    private lateinit var mockEngine: MockEngine
    private lateinit var walletRepo: WalletRepository
    private lateinit var connectionRepository: ConnectionRepository
    private lateinit var credentialRepository: CredentialRepository
    private lateinit var bpdServiceMocked: IBusinessPartnerDataService
    private lateinit var utilsService: UtilsService
    private lateinit var revocationServiceMocked: IRevocationService
    private lateinit var webhookServiceMocked: IWebhookService
    private lateinit var walletServiceSpy: IWalletService
    private lateinit var acaPyServiceMocked: IAcaPyService

    @BeforeTest
    fun setup() {
        walletRepo = WalletRepository()
        connectionRepository = ConnectionRepository()
        credentialRepository = CredentialRepository()
        mockEngine = MockEngine {
            respondBadRequest()
        }
        utilsService = UtilsService("")
        bpdServiceMocked = mock<IBusinessPartnerDataService>()
        webhookServiceMocked = mock<IWebhookService>()
        revocationServiceMocked = mock<IRevocationService>()
        runBlocking {
            whenever(revocationServiceMocked.issueStatusListCredentials(anyOrNull(), anyOrNull())).doAnswer{ }
            whenever(revocationServiceMocked.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
        }
        acaPyServiceMocked = mock<IAcaPyService>()
        whenever(acaPyServiceMocked.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
        doNothing().whenever(acaPyServiceMocked).subscribeBaseWalletForWebSocket()
        runBlocking {
            whenever(acaPyServiceMocked.setAuthorRoleAndInfoMetaData(any(), any(), any())).doAnswer{ }
            whenever(acaPyServiceMocked.setDidAsPublicUsingEndorser(any(), any())).doAnswer{ }
        }

        runBlocking {
            val connectionRecord = ConnectionRecord()
            connectionRecord.rfc23State = Rfc23State.RESPONSE_SENT.toString()
            whenever(acaPyServiceMocked.acceptConnectionRequest(any(), any()))
                .thenAnswer { connectionRecord }
        }
        val walletService = AcaPyWalletServiceImpl(
            acaPyServiceMocked,
            walletRepo,
            credentialRepository,
            utilsService,
            revocationServiceMocked,
            webhookServiceMocked,
            connectionRepository
        )
        walletServiceSpy = spy(walletService)
    }

    @Test
    fun testWebhookCallMissingWalletId() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureSecurity()
            configureOpenAPI()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(
                walletServiceSpy,
                bpdServiceMocked,
                revocationServiceMocked,
                webhookServiceMocked,
                utilsService
            )
            configureSerialization()
            configureStatusPages()
        }) {
            runBlocking {
                // Test x-wallet-id is missing in header
                handleRequest(HttpMethod.Post, "/webhook/topic/connections/") {
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("""{"test":"test"}""")
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                    assertTrue(response.content!!.contains("Missing or malformed walletId"))
                }
            }
        }
    }

    @Test
    fun testHandleAriesEventsConnections() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureSerialization()
        }) {
            runBlocking {
                // Setup
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))

                // Mock acceptInvitationRequest call to Acapy Service
                val connectionRecord  = ConnectionRecord()
                connectionRecord.rfc23State = Rfc23State.RESPONSE_SENT.toString()
                doAnswer { connectionRecord }
                    .whenever(acaPyServiceMocked).acceptConnectionRequest(any(), anyOrNull())
                val managedWalletAriesEventHandler = ManagedWalletsAriesEventHandler(
                    walletService = walletServiceSpy,
                    revocationService = revocationServiceMocked,
                    webhookService = webhookServiceMocked,
                    utilsService = utilsService
                )
                // Test `handleEvent` for connection with rfc23 state request-received
                assertDoesNotThrow {
                    managedWalletAriesEventHandler.handleEvent(
                        holderWallet.walletId,
                        "connections",
                        """{"rfc23_state":"request-received", "connection_id":"$connectionId"}""".trimIndent()
                    )
                }
                verify(walletServiceSpy, times(1)).acceptConnectionRequest(any(), any())

                // Test `handleEvent` for connection with rfc23 state completed
                assertDoesNotThrow {
                    managedWalletAriesEventHandler.handleEvent(
                        holderWallet.walletId,
                        "connections",
                        """
                            {
                                "rfc23_state":"completed",
                                "connection_id":"$connectionId",
                                "their_did":"shortdid123"
                            }
                        """.trimIndent()
                    )
                }
                verify(walletServiceSpy, times(1)).addConnection(any(), any(), any(), any())
                transaction {
                    val connections = connectionRepository.getConnections(holderWallet.did, "did:sov:shortdid123")
                    assertEquals(1, connections.size)
                    assertEquals(connectionId, connections[0].connectionId)
                }

                // clean data
                transaction {
                    walletRepo.deleteWallet(issuerWallet.bpn)
                    walletRepo.deleteWallet(holderWallet.bpn)
                    connectionRepository.deleteConnections(holderWallet.did)
                }
            }
        }
    }

    @Test
    fun testHandleAriesEventsConnectionsFromEndorser() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureSerialization()
        }) {
            runBlocking {
                // Setup
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))

                val managedWalletAriesEventHandler = ManagedWalletsAriesEventHandler(
                    walletService = walletServiceSpy,
                    revocationService = revocationServiceMocked,
                    webhookService = webhookServiceMocked,
                    utilsService = utilsService
                )

                transaction {
                    connectionRepository.add(
                        idOfConnection = connectionId,
                        connectionOwnerDid = holderWallet.did,
                        connectionTargetDid = issuerWallet.did,
                        rfc23State = "request-received"
                    )
                }

                // Test `handleEvent` for connection with rfc23 state completed
                assertDoesNotThrow {
                    managedWalletAriesEventHandler.handleEvent(
                        holderWallet.walletId,
                        "connections",
                        """
                            {
                                "rfc23_state":"completed",
                                "connection_id":"$connectionId",
                                "their_did":"${utilsService.getIdentifierOfDid(issuerWallet.did)}",
                                "their_role":"inviter",
                                "alias":"endorser"
                            }
                        """.trimIndent()
                    )
                }
                verify(walletServiceSpy, times(1)).setAuthorMetaData(any(), any())
                verify(walletServiceSpy, times(1)).setCommunicationEndpointUsingEndorsement(any())
                transaction {
                    val connections = connectionRepository.getConnections(holderWallet.did, issuerWallet.did)
                    assertEquals(1, connections.size)
                    assertEquals(connectionId, connections[0].connectionId)
                }

                // clean data
                transaction {
                    walletRepo.deleteWallet(issuerWallet.bpn)
                    walletRepo.deleteWallet(holderWallet.bpn)
                    connectionRepository.deleteConnections(holderWallet.did)
                }
            }
        }
    }

    @Test
    fun testHandleAriesEventsCredentials() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureSerialization()
        }) {
            runBlocking {
                // Setup
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))

                // Mock calls to AcaPy Service
                whenever(acaPyServiceMocked.acceptCredentialOfferBySendingRequest(any(), any(), any()))
                    .doReturn(Unit)
                whenever(acaPyServiceMocked.acceptCredentialReceivedByStoringIssuedCredential(any(), any(), any()))
                    .doReturn(Unit)

                val managedWalletAriesEventHandler = ManagedWalletsAriesEventHandler(
                    walletService = walletServiceSpy,
                    revocationService = revocationServiceMocked,
                    webhookService = webhookServiceMocked,
                    utilsService = utilsService
                )
                // Test `handleEvent` for connection
                val offerReceivedCredAsJson = File("./src/test/resources/credentials-test-data/credentialOffer.json")
                    .readText(Charsets.UTF_8)
                assertDoesNotThrow {
                    // "state": "offer_received"
                    managedWalletAriesEventHandler.handleEvent(
                        holderWallet.walletId,
                        "issue_credential_v2_0",
                        offerReceivedCredAsJson
                    )
                }
                verify(walletServiceSpy, times(1)).acceptReceivedOfferVc(any(), any())
                verify(walletServiceSpy, times(0)).acceptAndStoreReceivedIssuedVc(any(), any())

                val receivedIssuedCredAsJson = File("./src/test/resources/credentials-test-data/credentialReceived.json")
                    .readText(Charsets.UTF_8)
                assertDoesNotThrow {
                    // Test "state": "credential_received"
                    managedWalletAriesEventHandler.handleEvent(
                        holderWallet.walletId,
                        "issue_credential_v2_0",
                        receivedIssuedCredAsJson
                    )
                }
                verify(walletServiceSpy, times(1)).acceptAndStoreReceivedIssuedVc(any(), any())

                transaction{
                    val credentials = walletServiceSpy.getCredentials(null, null, null, null)
                    assertEquals(1, credentials.size)
                    assertEquals(credentialId, credentials[0].id)
                }

                // clean data
                transaction {
                    walletRepo.deleteWallet(issuerWallet.bpn)
                    walletRepo.deleteWallet(holderWallet.bpn)
                    credentialRepository.deleteCredentialByCredentialId(credentialId)
                }
            }
        }
    }

    private fun addWallets(
        walletRepo: WalletRepository,
        walletService: IWalletService,
        wallets: List<WalletExtendedData>
    ) {
        transaction {
            wallets.forEach {
                if (it.did == EnvironmentTestSetup.DEFAULT_DID) {
                    runBlocking {
                        walletService.initBaseWalletAndSubscribeForAriesWS(
                            EnvironmentTestSetup.DEFAULT_BPN,
                            EnvironmentTestSetup.DEFAULT_DID,
                            EnvironmentTestSetup.DEFAULT_VERKEY,
                            "Base-Wallet"
                        )
                    }
                } else {
                    walletRepo.addWallet(it)
                }
            }
        }
    }

}

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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.WalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletDtoParameter
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreatedSubWalletResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidResultDetails
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletSettings
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
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@kotlinx.serialization.ExperimentalSerializationApi
class WalletsTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.stop(1000, 10000)
    }

    @Test
    fun testWalletCrud() {
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
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(0, wallets.size)
            }

            // create wallet
            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"BPNL101", "name": "name1"}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(1, wallets.size)
            }

            // create second wallet
            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"${EnvironmentTestSetup.EXTRA_TEST_BPN}", "name": "testName1"}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // programmatically add a wallet
            runBlocking {
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto("did3", "name3"))
            }

            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(
                    WalletDto.serializer()), response.content!!
                )
                assertEquals(3, wallets.size)
                wallets.forEach {
                    assertEquals(true, it.pendingMembershipIssuance)
                }
            }

            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.EXTRA_TEST_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallet: WalletDto = Json.decodeFromString(WalletDto.serializer(), response.content!!)
                assertEquals(EnvironmentTestSetup.EXTRA_TEST_BPN, wallet.bpn)
            }

            val walletDtoParameter = WalletDtoParameter(EnvironmentTestSetup.EXTRA_TEST_BPN, true)
            assertEquals(EnvironmentTestSetup.EXTRA_TEST_BPN, walletDtoParameter.identifier)
            assertTrue { walletDtoParameter.withCredentials }
            // Get wallet with Cred
            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.EXTRA_TEST_BPN}?withCredentials=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallet: WalletDto = Json.decodeFromString(WalletDto.serializer(), response.content!!)
                assertEquals(EnvironmentTestSetup.EXTRA_TEST_BPN, wallet.bpn)
                assertTrue { wallet.vcs.isEmpty() }
            }

            // delete wallet from the store
            handleRequest(HttpMethod.Delete, "/api/wallets/BPNL101") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.DELETE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet("did3")
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.EXTRA_TEST_BPN)

                val listOfBpn = EnvironmentTestSetup.walletService.getAllBpns()
                assertEquals(0, listOfBpn.size)
            }

            // verify deletion
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(0, wallets.size)
            }

            assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
        }
    }

    @Test
    fun testWalletCrudExceptions() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureSecurity()
            configureOpenAPI()
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

            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            // create base wallet
            runBlocking {
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name1"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("wrong:json")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("wrong"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"wrong":"json"}""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("required"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":null}""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("null"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn2", "name": null}""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("null"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"", "name": "name2"}""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("Field 'bpn' is required not to be blank, but it was blank"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn2", "name": ""}""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue(response.content!!.contains("Field 'name' is required not to be blank, but it was blank"))
            }

            // programmatically add a wallet
            runBlocking {
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto("bpn4", "name4"))
            }

            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"bpn4", "name": "name4"}""")
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
                assertTrue(response.content!!.contains("Wallet with given identifier already exists!"))
            }

            // clean up created wallets
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet("bpn4")
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // Base wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }
        }
    }

    @Test
    fun testInitBaseWallet() {
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
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {

            runBlocking {
                EnvironmentTestSetup.walletService.initBaseWalletAndSubscribeForAriesWS(
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Base_Wallet"
                )
            }

            val didOfWallet = EnvironmentTestSetup.walletService.getDidFromBpn(EnvironmentTestSetup.DEFAULT_BPN)
            val bpnOfWallet = EnvironmentTestSetup.walletService.getBpnFromDid(didOfWallet)
            assertTrue { bpnOfWallet == EnvironmentTestSetup.DEFAULT_BPN}
            var bpnOfWalletUsingIdentifier = EnvironmentTestSetup.walletService.getBpnFromIdentifier(didOfWallet)
            assertTrue { bpnOfWalletUsingIdentifier == EnvironmentTestSetup.DEFAULT_BPN}
            bpnOfWalletUsingIdentifier = EnvironmentTestSetup.walletService.getBpnFromIdentifier(EnvironmentTestSetup.DEFAULT_BPN)
            assertTrue { bpnOfWalletUsingIdentifier == EnvironmentTestSetup.DEFAULT_BPN}

            val wallet = EnvironmentTestSetup.walletService.getBaseWallet()
            assertTrue { wallet.bpn == EnvironmentTestSetup.DEFAULT_BPN }
            assertTrue { wallet.did == EnvironmentTestSetup.DEFAULT_DID }
            assertNull(wallet.walletId )

            // clean up created wallets
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }
        }
    }


    @Test
    fun testCreateWalletCalls() { // same as public
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            transaction {
                runBlocking {
                    val acapyService = mock<IAcaPyService>()
                    whenever(acapyService.getWalletAndAcaPyConfig()).thenReturn(
                        WalletAndAcaPyConfig(
                            "networkId",
                            EnvironmentTestSetup.DEFAULT_BPN,
                            EnvironmentTestSetup.DEFAULT_DID,
                            EnvironmentTestSetup.DEFAULT_VERKEY,
                            "apiAdminUrl",
                            "AdminApiKey",
                            "",
                            "",
                        )
                    )
                    whenever(acapyService.createSubWallet(any()))
                    .thenReturn(
                        CreatedSubWalletResult(
                            "createdAt",
                            "wallet_id_extrawallet",
                            "managed",
                            "updated_at",
                            WalletSettings(
                                "askar",
                                "extraWallet",
                                emptyList(),
                                "base",
                                "wallet_id_extrawallet",
                                "label",
                                ""
                            ),
                            "wallet_token_extrawallet"
                        )
                    )

                    whenever(acapyService.createLocalDidForWallet(any(), any()))
                        .thenReturn(
                            DidResult(
                                DidResultDetails(
                                    "extrawallet1did", "", "", "", "verkey_extrawallet"
                                )
                            )
                        )
                    whenever(acapyService.registerDidOnLedgerUsingBaseWallet(any()))
                        .thenAnswer {  }
                    whenever(acapyService.sendConnectionRequest(any(), any(), any(), any(), any()))
                        .thenAnswer {  }
                    val walletRepository = WalletRepository()
                    val connectionRepository = ConnectionRepository()
                    val credentialRepository = CredentialRepository()
                    val utilsService = UtilsService("")
                    val revocationService = mock<IRevocationService>()
                    whenever(revocationService.registerList(any(), any()))
                        .thenReturn("revocation-list-${EnvironmentTestSetup.EXTRA_TEST_BPN}")
                    val webhookService = mock<IWebhookService>()
                    val walletService = AcaPyWalletServiceImpl(
                        acapyService,
                        walletRepository,
                        credentialRepository,
                        utilsService,
                        revocationService,
                        webhookService,
                        connectionRepository
                    )
                    val walletServiceSpy = spy(walletService)

                    EnvironmentTestSetup.walletService.initBaseWalletAndSubscribeForAriesWS(
                        EnvironmentTestSetup.DEFAULT_BPN,
                        EnvironmentTestSetup.DEFAULT_DID,
                        EnvironmentTestSetup.DEFAULT_VERKEY,
                        "Base_Wallet"
                    )

                    walletServiceSpy.createWallet(
                        WalletCreateDto(EnvironmentTestSetup.EXTRA_TEST_BPN, "extraWallet")
                    )
                    var listOfWallets = walletServiceSpy.getAll()
                    assertEquals(2, listOfWallets.size)
                    assertEquals("did:sov:extrawallet1did", listOfWallets[1].did)

                    verify(acapyService, times(1)).createSubWallet(any())
                    verify(acapyService, times(1)).createLocalDidForWallet(any(), any())
                    verify(acapyService, times(1)).registerDidOnLedgerUsingBaseWallet(any())
                    verify(acapyService, times(1)).sendConnectionRequest(
                        any(), any(), any(), any(), any()
                    )

                    walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
                    walletService.deleteWallet(EnvironmentTestSetup.EXTRA_TEST_BPN)
                    listOfWallets = walletServiceSpy.getAll()
                    assertEquals(0, listOfWallets.size)
                }
            }
        }
    }
}
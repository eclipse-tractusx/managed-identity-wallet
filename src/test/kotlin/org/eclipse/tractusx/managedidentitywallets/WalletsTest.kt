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
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import kotlin.test.*

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
    fun testWalletCrud() { // true
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
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
                setBody("""{"bpn":"${EnvironmentTestSetup.DEFAULT_BPN}", "name": "name1"}""")
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

            // create wallet (not Catena X)
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
                val wallets: List<WalletDto> = Json.decodeFromString(ListSerializer(WalletDto.serializer()), response.content!!)
                assertEquals(3, wallets.size)
                wallets.forEach {
                    assertEquals(false, it.pendingMembershipIssuance)
                }
            }

            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallet: WalletDto = Json.decodeFromString(WalletDto.serializer(), response.content!!)
                assertEquals(EnvironmentTestSetup.DEFAULT_BPN, wallet.bpn)
            }

            val walletDtoParameter = WalletDtoParameter(EnvironmentTestSetup.DEFAULT_BPN, true)
            assertEquals(EnvironmentTestSetup.DEFAULT_BPN, walletDtoParameter.identifier)
            assertTrue { walletDtoParameter.withCredentials }
            // Get wallet with Cred
            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}?withCredentials=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val wallet: WalletDto = Json.decodeFromString(WalletDto.serializer(), response.content!!)
                assertEquals(EnvironmentTestSetup.DEFAULT_BPN, wallet.bpn)
                assertTrue { wallet.vcs.isEmpty() }
            }

            // delete wallet from the store
            handleRequest(HttpMethod.Delete, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
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
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // Catena-X wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }
        }
    }

    @Test
    fun testRegisterBaseWallet() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {

            var verKey: String

            // programmatically add base wallet and an additional one
            runBlocking {
                val createdWallet = EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "base")
                )
                verKey = createdWallet.verKey!!
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto("non_base_bpn", "non_base"))
            }

            val didOfWallet = EnvironmentTestSetup.walletService.getDidFromBpn(EnvironmentTestSetup.DEFAULT_BPN)
            val bpnOfWallet = EnvironmentTestSetup.walletService.getBpnFromDid(didOfWallet)
            assertTrue { bpnOfWallet ==EnvironmentTestSetup.DEFAULT_BPN}
            var bpnOfWalletUsingIdentifier = EnvironmentTestSetup.walletService.getBpnFromIdentifier(didOfWallet)
            assertTrue { bpnOfWalletUsingIdentifier ==EnvironmentTestSetup.DEFAULT_BPN}
            bpnOfWalletUsingIdentifier = EnvironmentTestSetup.walletService.getBpnFromIdentifier(EnvironmentTestSetup.DEFAULT_BPN)
            assertTrue { bpnOfWalletUsingIdentifier ==EnvironmentTestSetup.DEFAULT_BPN}

            var exception = assertFailsWith<NotFoundException> {
                handleRequest(HttpMethod.Post, "/api/wallets/non_existing_bpn/public") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("{\"verKey\":\"$verKey\"}")
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, response.status())
                }
            }
            assertTrue(exception.message!!.contains("not found"))

            exception = assertFailsWith<NotFoundException> {
                handleRequest(HttpMethod.Post, "/api/wallets/non_base_bpn/public") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody("{\"verKey\":\"$verKey\"}")
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, response.status())
                }
            }
            assertTrue(exception.message!!.contains("wallet but the base wallet"))

            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/public") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("{\"verKey\":\"$verKey\"}")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // clean up created wallets
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                EnvironmentTestSetup.walletService.deleteWallet("non_base_bpn")
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }

        }
    }

}
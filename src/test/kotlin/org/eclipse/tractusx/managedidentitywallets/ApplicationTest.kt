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

import kotlinx.coroutines.*
import kotlin.test.*

import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.routes.*

@kotlinx.serialization.ExperimentalSerializationApi
class ApplicationTest {

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
    fun testRoleMapping() {
        var exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentWithMissingRoleMapping(environment)
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_VIEW_WALLETS} role mapping not defined"))

        exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentRoleMapping(environment, "view_wallets:view_wallets")
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_CREATE_WALLETS} role mapping not defined"))

        exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentRoleMapping(
                    environment,
                    "view_wallets:view_wallets,create_wallets:create:wallets")
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_UPDATE_WALLETS} role mapping not defined"))

        exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentRoleMapping(
                    environment,
                    "view_wallets:view_wallets,create_wallets:create:wallets,update_wallets:update_wallets")
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_DELETE_WALLETS} role mapping not defined"))

        exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentRoleMapping(
                    environment,
                    "view_wallets:view_wallets,create_wallets:create:wallets,update_wallets:update_wallets" +
                            ",delete_wallets:delete_wallets")
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_UPDATE_WALLET} role mapping not defined"))

        exception = assertFailsWith<Exception> {
            withTestApplication({
                EnvironmentTestSetup.setupEnvironmentRoleMapping(
                    environment,
                    "view_wallets:view_wallets,create_wallets:create:wallets,update_wallets:update_wallets" +
                            ",delete_wallets:delete_wallets,update_wallet:update_wallet")
                configureSecurity()
            }) { }
        }
        assertTrue(exception.message!!.contains("${AuthorizationHandler.ROLE_VIEW_WALLET} role mapping not defined"))
    }

    @Test
    fun testSchedulerConfig() {
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
            configureJobs()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {
            assertTrue(true)
        }
    }

    @Test
    fun testRoot() {
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
            val businessPartnerInfo = BusinessPartnerInfo(EnvironmentTestSetup.DEFAULT_BPN)
            assertEquals(EnvironmentTestSetup.DEFAULT_BPN, businessPartnerInfo.bpn)

            val userSession = UserSession("token")
            assertEquals("token", userSession.token)
        }
    }

    @Test
    fun testSocketInstall() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configureSockets()
        }) {
            assertTrue(true)
        }
    }

    @Test
    fun testWithWrongRoles() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configureStatusPages()
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

            assertFails {
                handleRequest(HttpMethod.Get, "/api/wallets") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.EMPTY_ROLES_TOKEN}")
                    addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }.apply{
                    assertTrue { false }
                }
            }

            // view wallets with wrong token should not work
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("It has none of the sufficient role(s) view_wallet or view_wallets") }
            }

            // create wallet with wrong token should not work
            handleRequest(HttpMethod.Post, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"bpn":"${EnvironmentTestSetup.DEFAULT_BPN}", "name": "name1"}""")
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("It has none of the sufficient role(s) create_wallets") }
            }

            // programmatically add base wallet
            runBlocking {
                 EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name1")).verKey
            }

            // delete should not work with wrong token
            handleRequest(HttpMethod.Delete, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.CREATE_TOKEN}")
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("It has none of the sufficient role(s) delete_wallets") }
            }

            // delete should not work without token
            handleRequest(HttpMethod.Delete, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }

            // clean up
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }

            assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
        }

    }

    @Test
    fun testWithSingleRoles() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configureStatusPages()
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

            // view wallets with single view token should not work
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertTrue { response.content!!.contains("The Identifier is mandatory for view_wallet role") }
            }

            val didOfDefaultBpn: String
            val didOfExtraBpn: String
            // programmatically add base wallet
            runBlocking {
                didOfDefaultBpn = EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "default_name")
                ).did
                didOfExtraBpn = EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.EXTRA_TEST_BPN, "test_name")
                ).did
            }

            // view single wallet should work
            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            // view single wallet without BPN should not work
            handleRequest(HttpMethod.Get, "/api/wallets") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN_SINGLE_WITHOUT_BPN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertTrue { response.content!!.contains("The Identifier is mandatory for view_wallet role") }
            }

            // view wallet with different BPN
            handleRequest(HttpMethod.Get, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN_SINGLE_EXTRA_BPN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("Wallet BPN ${EnvironmentTestSetup.DEFAULT_BPN} does " +
                        "not match requestors BPN ${EnvironmentTestSetup.EXTRA_TEST_BPN}") }
            }

            // request a credential by the Catena-X issuer
            handleRequest(HttpMethod.Post, "/api/credentials/issuer") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1"
    ],
    "id": "http://example.edu/credentials/3735",
    "type": [
        "University-Degree-Credential",
        "VerifiableCredential"
    ],
    "issuerIdentifier": "${EnvironmentTestSetup.DEFAULT_BPN}",
    "issuanceDate": "2021-06-16T18:56:59Z",
    "expirationDate": "2026-06-17T18:56:59Z",
    "credentialSubject": {
        "givenName": "TestAfterQuestion",
        "familyName": "Student",
        "degree": {
            "type": "Master",
            "degreeType": "Undergraduate",
            "name": "Master of Test"
        },
        "college": "Test"
    },
    "holderIdentifier": "${EnvironmentTestSetup.DEFAULT_BPN}"
}
""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue { response.content!!.contains("Error: no verification methods") }
            }

            // request a Catena-X credential using not Catena-X BPN in Token
            handleRequest(HttpMethod.Post, "/api/credentials/issuer") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE_EXTRA_BPN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1"
    ],
    "id": "http://example.edu/credentials/3735",
    "type": [
        "University-Degree-Credential",
        "VerifiableCredential"
    ],
    "issuerIdentifier": "${EnvironmentTestSetup.EXTRA_TEST_BPN}",
    "issuanceDate": "2021-06-16T18:56:59Z",
    "expirationDate": "2026-06-17T18:56:59Z",
    "credentialSubject": {
        "givenName": "TestAfterQuestion",
        "familyName": "Student",
        "degree": {
            "type": "Master",
            "degreeType": "Undergraduate",
            "name": "Master of Test"
        },
        "college": "Test"
    },
    "holderIdentifier": "${EnvironmentTestSetup.EXTRA_TEST_BPN}"
}
""")
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("Wallet BPN ${EnvironmentTestSetup.DEFAULT_BPN} " +
                        "does not match requestors BPN ${EnvironmentTestSetup.EXTRA_TEST_BPN}") }
            }

            // request a presentation
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "holderIdentifier": "${EnvironmentTestSetup.DEFAULT_BPN}",
    "verifiableCredentials": [
        {
            "id": "http://example.edu/credentials/3732",
            "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
            ],
            "type": [
                "University-Degree-Credential",
                "VerifiableCredential"
            ],
            "issuer": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}43Arq24V9uQFPKHuDb7TC5",
            "issuanceDate": "2019-06-16T18:56:59Z",
            "expirationDate": "2019-06-17T18:56:59Z",
            "credentialSubject": {
                "givenName": "Sally",
                "familyName": "Student",
                "degree": {
                    "type": "Master",
                    "degreeType": "Undergraduate",
                    "name": "Master of Science and Arts"
                },
                "college": "Stuttgart",
                "id": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}QZakhgHUUAowUbhgZ9PZLD"
            },
            "proof": {
                "type": "Ed25519Signature2018",
                "created": "2022-03-24T09:34:02Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}43Arq24V9uQFPKHuDb7TC5#key-1",
                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..LvCQ4TWhFHOkwMzvrx-TxHovoaCLPlK2taHxQUtUOp0Uc_jYbjL3XgVR2u6jVMvGIdPt4gs-VZb49f7GuiXFDA"
            }
        }
    ]
}
""")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertTrue { response.content!!.contains("Error: no verification methods") }
            }

            // request a presentation, wrong authorization
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "holderIdentifier": "${EnvironmentTestSetup.EXTRA_TEST_BPN}",
    "verifiableCredentials": [
        {
            "id": "http://example.edu/credentials/3732",
            "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
            ],
            "type": [
                "University-Degree-Credential",
                "VerifiableCredential"
            ],
            "issuer": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}43Arq24V9uQFPKHuDb7TC5",
            "issuanceDate": "2019-06-16T18:56:59Z",
            "expirationDate": "2019-06-17T18:56:59Z",
            "credentialSubject": {
                "givenName": "Sally",
                "familyName": "Student",
                "degree": {
                    "type": "Master",
                    "degreeType": "Undergraduate",
                    "name": "Master of Science and Arts"
                },
                "college": "Stuttgart",
                "id": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}QZakhgHUUAowUbhgZ9PZLD"
            },
            "proof": {
                "type": "Ed25519Signature2018",
                "created": "2022-03-24T09:34:02Z",
                "proofPurpose": "assertionMethod",
                "verificationMethod": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}43Arq24V9uQFPKHuDb7TC5#key-1",
                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..LvCQ4TWhFHOkwMzvrx-TxHovoaCLPlK2taHxQUtUOp0Uc_jYbjL3XgVR2u6jVMvGIdPt4gs-VZb49f7GuiXFDA"
            }
        }
    ]
}
""")
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("Wallet BPN ${EnvironmentTestSetup.EXTRA_TEST_BPN} " +
                        "does not match requestors BPN ${EnvironmentTestSetup.DEFAULT_BPN}") }
            }

            // request to store credential by holder and correct BPN in Token
            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "id": "http://example.edu/credentials/3666",
    "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1"
    ],
    "type": [
        "University-Degree-Credential",
        "VerifiableCredential"
    ],
    "issuer": "$didOfExtraBpn",
    "issuanceDate": "2025-06-16T18:56:59Z",
    "expirationDate": "2026-06-17T18:56:59Z",
    "credentialSubject": {
        "givenName": "TestAfterQuestion",
        "familyName": "Student",
        "degree": {
            "type": "Master1",
            "degreeType": "Undergraduate2",
            "name": "Master of Test11"
        },
        "college": "Test2",
        "id": "$didOfDefaultBpn"
    },
    "proof": {
        "type": "Ed25519Signature2018",
        "created": "2022-07-15T09:35:59Z",
        "proofPurpose": "assertionMethod",
        "verificationMethod": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}JPbsf8GpUYiavsK95SGpge#key-1",
        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..4mFcySYFNAV6Bif6OqHeGqhQZ1kPMbq5FbOjurbIBIyYnQyRICa1b7RB_nxfz9fdP7WYxthTVnaWiXs2WbpzBQ"
    }
}
""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // request to store credential, wrong authorization
            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_SINGLE_EXTRA_BPN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "id": "http://example.edu/credentials/3666",
    "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1"
    ],
    "type": [
        "University-Degree-Credential",
        "VerifiableCredential"
    ],
    "issuer": "$didOfExtraBpn",
    "issuanceDate": "2025-06-16T18:56:59Z",
    "expirationDate": "2026-06-17T18:56:59Z",
    "credentialSubject": {
        "givenName": "TestAfterQuestion",
        "familyName": "Student",
        "degree": {
            "type": "Master1",
            "degreeType": "Undergraduate2",
            "name": "Master of Test11"
        },
        "college": "Test2",
        "id": "$didOfDefaultBpn"
    },
    "proof": {
        "type": "Ed25519Signature2018",
        "created": "2022-07-15T09:35:59Z",
        "proofPurpose": "assertionMethod",
        "verificationMethod": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}JPbsf8GpUYiavsK95SGpge#key-1",
        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..4mFcySYFNAV6Bif6OqHeGqhQZ1kPMbq5FbOjurbIBIyYnQyRICa1b7RB_nxfz9fdP7WYxthTVnaWiXs2WbpzBQ"
    }
}
""")
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
                assertTrue { response.content!!.contains("Wallet BPN ${EnvironmentTestSetup.DEFAULT_BPN} " +
                        "does not match requestors BPN ${EnvironmentTestSetup.EXTRA_TEST_BPN}") }
            }

            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN_ALL_AND_SINGLE_EXTRA_BPN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    """
{
    "id": "http://example.edu/credentials/3111",
    "@context": [
        "https://www.w3.org/2018/credentials/v1",
        "https://www.w3.org/2018/credentials/examples/v1"
    ],
    "type": [
        "University-Degree-Credential",
        "VerifiableCredential"
    ],
    "issuer": "$didOfExtraBpn",
    "issuanceDate": "2025-06-16T18:56:59Z",
    "expirationDate": "2026-06-17T18:56:59Z",
    "credentialSubject": {
        "givenName": "TestAfterQuestion",
        "familyName": "Student",
        "degree": {
            "type": "Master1",
            "degreeType": "Undergraduate2",
            "name": "Master of Test11"
        },
        "college": "Test2",
        "id": "$didOfDefaultBpn"
    },
    "proof": {
        "type": "Ed25519Signature2018",
        "created": "2022-07-15T09:35:59Z",
        "proofPurpose": "assertionMethod",
        "verificationMethod": "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}JPbsf8GpUYiavsK95SGpge#key-1",
        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..4mFcySYFNAV6Bif6OqHeGqhQZ1kPMbq5FbOjurbIBIyYnQyRICa1b7RB_nxfz9fdP7WYxthTVnaWiXs2WbpzBQ"
    }
}
""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // clean up
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.EXTRA_TEST_BPN)
                val credentials = EnvironmentTestSetup.walletService.getCredentials(
                    null, null, null, null
                )
                assertEquals(0, credentials.size)
            }

            assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
        }
    }

}

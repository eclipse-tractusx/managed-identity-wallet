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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.ssi.CredentialStatus
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.ssi.LdProofDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.WithDateValidation
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.plugins.configureOpenAPI
import org.eclipse.tractusx.managedidentitywallets.plugins.configurePersistence
import org.eclipse.tractusx.managedidentitywallets.plugins.configureRouting
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSecurity
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSerialization
import org.eclipse.tractusx.managedidentitywallets.plugins.configureStatusPages
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@kotlinx.serialization.ExperimentalSerializationApi
class PresentationsTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.stop(1000, 10000)
    }

    @AfterTest
    fun cleanSingletonTestData() {
        SingletonTestData.cleanSingletonTestData()
    }

    @Test
    fun testIssuePresentation() {
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
                    "Base-Wallet"
                )
                SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
            }

            val invalidDID = SingletonTestData.baseWalletDID
                .replace(
                    SingletonTestData.getDidMethodPrefixWithNetworkIdentifier(),
                    "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()} WRONG"
                )
            val verifiablePresentationRequestWithInvalidDIDs = VerifiablePresentationRequestDto(
                holderIdentifier = SingletonTestData.baseWalletDID,
                verifiableCredentials = listOf(
                    VerifiableCredentialDto(
                        context = listOf(
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                        ),
                        id = "http://example.edu/credentials/333",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        issuer = invalidDID,
                        issuanceDate = "2019-06-16T18:56:59Z",
                        expirationDate = "2999-06-17T18:56:59Z",
                        credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                        proof = LdProofDto(
                            type = "Ed25519Signature2018",
                            created = "2021-11-17T22:20:27Z",
                            proofPurpose = "assertionMethod",
                            verificationMethod = "$invalidDID#key-1",
                            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                        )
                    )
                )
            )

           handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequestWithInvalidDIDs))
           }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
           }

            // With invalid issuanceDate
            val verifiablePresentationRequestWithInvalidDate = VerifiablePresentationRequestDto(
                holderIdentifier = SingletonTestData.baseWalletDID,
                verifiableCredentials = listOf(
                    VerifiableCredentialDto(
                        context = listOf(
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                        ),
                        id = "http://example.edu/credentials/333",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        issuer = SingletonTestData.baseWalletDID,
                        issuanceDate = "2999-06-16T18:56:59Z",
                        expirationDate = "2999-06-17T18:56:59Z",
                        credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                        proof = LdProofDto(
                            type = "Ed25519Signature2018",
                            created = "2021-11-17T22:20:27Z",
                            proofPurpose = "assertionMethod",
                            verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                        )
                    )
                )
            )

            // Validate Credential and its Dates
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequestWithInvalidDate))
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            val verifiablePresentationRequest = VerifiablePresentationRequestDto(
                holderIdentifier = SingletonTestData.baseWalletDID,
                verifiableCredentials = listOf(
                    VerifiableCredentialDto(
                        context = listOf(
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                        ),
                        id = "http://example.edu/credentials/333",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        issuer = SingletonTestData.baseWalletDID,
                        issuanceDate = "2019-06-16T18:56:59Z",
                        expirationDate = "2999-06-17T18:56:59Z",
                        credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                        proof = LdProofDto(
                            type = "Ed25519Signature2018",
                            created = "2021-11-17T22:20:27Z",
                            proofPurpose = "assertionMethod",
                            verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                        )
                    )
                )
            )

            val signedCred = Json.encodeToString(
                VerifiablePresentationDto.serializer(),
                VerifiablePresentationDto(
                    context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
                    type =  listOf("VerifiablePresentation"),
                    holder = SingletonTestData.baseWalletDID,
                    verifiableCredential = listOf(
                        VerifiableCredentialDto(
                            context = listOf(
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                            ),
                            id = "http://example.edu/credentials/3732",
                            type = listOf("University-Degree-Credential, VerifiableCredential"),
                            issuer = SingletonTestData.baseWalletDID,
                            issuanceDate = "2019-06-16T18:56:59Z",
                            expirationDate = "2999-06-17T18:56:59Z",
                            credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                            proof = LdProofDto(
                                type = "Ed25519Signature2018",
                                created = "2021-11-17T22:20:27Z",
                                proofPurpose = "assertionMethod",
                                verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                                jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                            )
                        )
                    ),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )

            // issue presentation for verifiablePresentationRequestWithInvalidDate without checking date
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""
            SingletonTestData.isValidVerifiableCredential = true
            handleRequest(HttpMethod.Post, "/api/presentations?withCredentialsDateValidation=false") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequestWithInvalidDate))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // Verifiable Credential is not valid
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""
            SingletonTestData.isValidVerifiableCredential = false
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            // Good Case: Verifiable Credential is valid
            SingletonTestData.isValidVerifiableCredential = true
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // withCredentialsDateValidation will be ignored
            SingletonTestData.isValidVerifiableCredential = false
            handleRequest(HttpMethod.Post, "/api/presentations?" +
                    "withCredentialsValidation=false&withCredentialsDateValidation=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequestWithInvalidDate))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // Ignore credential validation / withCredentialsDateValidation is false
            SingletonTestData.isValidVerifiableCredential = false
            handleRequest(HttpMethod.Post, "/api/presentations?" +
                    "withCredentialsValidation=false&withCredentialsDateValidation=false") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // Empty signed Presentation
            val nullValue = null
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $nullValue, "error": "mocked Error" }"""
            SingletonTestData.isValidVerifiableCredential = false
            handleRequest(HttpMethod.Post, "/api/presentations?withCredentialsValidation=false") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                    verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }

            // clean up created wallet and singletonTestData
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssuePresentationForRevokedCredential() {
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
                    "Base-Wallet"
                )
                val walletDto =  EnvironmentTestSetup.walletService.getWallet(EnvironmentTestSetup.DEFAULT_BPN)
                SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
                SingletonTestData.revocationListName = walletDto.revocationListName!!
                SingletonTestData.credentialIndex = 3
            }

            val verifiablePresentationRequest = VerifiablePresentationRequestDto(
                holderIdentifier = SingletonTestData.baseWalletDID,
                verifiableCredentials = listOf(
                    VerifiableCredentialDto(
                        context = listOf(
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                        ),
                        id = "http://example.edu/credentials/333",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        issuer = SingletonTestData.baseWalletDID,
                        issuanceDate = "2019-06-16T18:56:59Z",
                        expirationDate = "2999-06-17T18:56:59Z",
                        credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                        credentialStatus = CredentialStatus(
                            statusId = "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                            credentialType  = "StatusList2021Entry",
                            statusPurpose = "revocation",
                            index = "${SingletonTestData.credentialIndex}",
                            listUrl = "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                        ),
                        proof = LdProofDto(
                            type = "Ed25519Signature2018",
                            created = "2021-11-17T22:20:27Z",
                            proofPurpose = "assertionMethod",
                            verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                        )
                    )
                )
            )

            val signedCred = Json.encodeToString(
                VerifiablePresentationDto.serializer(),
                VerifiablePresentationDto(
                    context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
                    type =  listOf("VerifiablePresentation"),
                    holder = SingletonTestData.baseWalletDID,
                    verifiableCredential = listOf(
                        VerifiableCredentialDto(
                            context = listOf(
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                            ),
                            id = "http://example.edu/credentials/333",
                            type = listOf("University-Degree-Credential, VerifiableCredential"),
                            issuer = SingletonTestData.baseWalletDID,
                            issuanceDate = "2019-06-16T18:56:59Z",
                            expirationDate = "2999-06-17T18:56:59Z",
                            credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
                            credentialStatus = CredentialStatus(
                                statusId = "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                credentialType  = "StatusList2021Entry",
                                statusPurpose = "revocation",
                                index = "${SingletonTestData.credentialIndex}",
                                listUrl = "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            ),
                            proof = LdProofDto(
                                type = "Ed25519Signature2018",
                                created = "2021-11-17T22:20:27Z",
                                proofPurpose = "assertionMethod",
                                verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                                jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                            )
                        )
                    ),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )

            // Good Case: Verifiable Credential is valid
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""
            SingletonTestData.isValidVerifiableCredential = true
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            SingletonTestData.encodedList = EnvironmentTestSetup.ZERO_THIRD_REVOKED_ENCODED_LIST
            handleRequest(HttpMethod.Post, "/api/presentations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("The credential http://example.edu/credentials/333 has been revoked!"))
            }

            // ignore revocation
            handleRequest(HttpMethod.Post, "/api/presentations?withRevocationValidation=false") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiablePresentationRequestDto.serializer(),
                        verifiablePresentationRequest))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            // clean up created wallet and singletonTestData
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testVerifyPresentation() {
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
            // programmatically add base wallet
            runBlocking {
                EnvironmentTestSetup.walletService.initBaseWalletAndSubscribeForAriesWS(
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Base-Wallet"
                )
            }
            //TODO replace did:sov in all used json files when indy did method is supported by AcaPy
            val validVP = File("./src/test/resources/presentations-test-data/validVP.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validVP)
            }.apply {
                val output = Json.decodeFromString<VerifyResponse>(response.content!!)
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { output.valid }
            }

            val vpWithInvalidDID = File("./src/test/resources/presentations-test-data/vpWithInvalidDID.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithInvalidDID)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            val vpWithoutProof = File("./src/test/resources/presentations-test-data/vpWithoutProof.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithoutProof)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("Cannot verify verifiable presentation due to missing proof"))
            }

            val vpWithOutdatedVC = File("./src/test/resources/presentations-test-data/vpWithOutdatedVC.json").readText(Charsets.UTF_8)
            val withDateValidation = WithDateValidation()
            assertFalse { withDateValidation.withDateValidation!! }

            handleRequest(HttpMethod.Post, "/api/presentations/validation?withDateValidation=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithOutdatedVC)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains(
                    "Verifiable credential http://example.edu/credentials/3735 expired 2021-06-17T18:56:59Z"))
            }

            val vpWithFutureVC = File("./src/test/resources/presentations-test-data/vpWithFutureVC.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation?withDateValidation=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithFutureVC)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains(
                    "Invalid issuance date 2999-06-16T18:56:59Z " +
                            "in verifiable credential http://example.edu/credentials/3888"))
            }

            val vpWithVcWithoutProof = File("./src/test/resources/presentations-test-data/vpWithVcWithoutProof.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithVcWithoutProof)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains(
                    "Cannot verify verifiable credential" +
                            " http://example.edu/credentials/3735 due to missing proof"))
            }

            val vpWithoutHolder: String = File("./src/test/resources/presentations-test-data/vpWithoutHolder.json").readText(Charsets.UTF_8)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithoutHolder)
            }.apply {
                val output = Json.decodeFromString<VerifyResponse>(response.content!!)
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { output.valid }
            }

            // mocked valid signature for presentation returns false
            SingletonTestData.isValidVerifiablePresentation = false
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validVP)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }
            SingletonTestData.isValidVerifiablePresentation = true

            // mocked valid signature for verifiable credential returns false
            SingletonTestData.isValidVerifiableCredential = false
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validVP)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }

            // clean up created wallets
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }
        }
    }

    @Test
    fun testVerifyPresentationWithRevocationCheck() {
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
            // programmatically add base wallet
            runBlocking {
                EnvironmentTestSetup.walletService.initBaseWalletAndSubscribeForAriesWS(
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Base-Wallet"
                )
                val wallet = EnvironmentTestSetup.walletService.getWallet(EnvironmentTestSetup.DEFAULT_DID)
                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
                SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
                SingletonTestData.credentialIndex = 3
                SingletonTestData.revocationListName = wallet.revocationListName!!
            }

            val vpWithPlaceholders = File("./src/test/resources/presentations-test-data/vpWithPlaceholders.json").readText(Charsets.UTF_8)
            val validVp = vpWithPlaceholders.replace("<issuer-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>",
                "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}")
                .replace("<index-to-replace>", SingletonTestData.credentialIndex.toString())
                .replace("<status-type-to-replace>", CredentialStatus.CREDENTIAL_TYPE)
                .replace("<status-purpose-to-replace>", CredentialStatus.STATUS_PURPOSE)
            SingletonTestData.encodedList = EnvironmentTestSetup.NONE_REVOKED_ENCODED_LIST
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validVp)
            }.apply {
                val output = Json.decodeFromString<VerifyResponse>(response.content!!)
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { output.valid }
            }

            SingletonTestData.encodedList = EnvironmentTestSetup.ZERO_THIRD_REVOKED_ENCODED_LIST
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(validVp)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("The credential http://example.edu/credentials/3735 has been revoked!"))
            }

            val vpWithWrongStatusType = vpWithPlaceholders.replace("<issuer-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>",
                "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}")
                .replace("<index-to-replace>", SingletonTestData.credentialIndex.toString())
                .replace("<status-type-to-replace>", "WRONG_TYPE")
                .replace("<status-purpose-to-replace>", CredentialStatus.STATUS_PURPOSE)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithWrongStatusType)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid credential status 'Type'"))
            }

            val vpWithWrongStatusPurpose = vpWithPlaceholders.replace("<issuer-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>",
                "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}")
                .replace("<index-to-replace>", SingletonTestData.credentialIndex.toString())
                .replace("<status-type-to-replace>", CredentialStatus.CREDENTIAL_TYPE)
                .replace("<status-purpose-to-replace>", "WRONG_PURPOSE")
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithWrongStatusPurpose)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusPurpose'"))
            }

            val vpWithEmptyStatusListIndex =
                vpWithPlaceholders.replace("<issuer-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>",
                "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}")
                .replace("<index-to-replace>", "  ")
                .replace("<status-type-to-replace>", CredentialStatus.CREDENTIAL_TYPE)
                .replace("<status-purpose-to-replace>", CredentialStatus.STATUS_PURPOSE)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithEmptyStatusListIndex)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListIndex'"))
            }

            val vpWithEmptyStatusListUrl = vpWithPlaceholders.replace("<issuer-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>", "  ")
                .replace("<index-to-replace>", SingletonTestData.credentialIndex.toString())
                .replace("<status-type-to-replace>", CredentialStatus.CREDENTIAL_TYPE)
                .replace("<status-purpose-to-replace>", CredentialStatus.STATUS_PURPOSE)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithEmptyStatusListUrl)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListCredential'"))
            }

            val vpWithIssuerConflict =
                vpWithPlaceholders.replace("<issuer-to-replace>",
                    "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}AA5EEDcn8yTfMobaTcabj9")
                .replace("<verificationMethod-to-replace>", SingletonTestData.baseWalletDID)
                .replace("<revocation-list-to-replace>",
                "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}")
                .replace("<index-to-replace>", SingletonTestData.credentialIndex.toString())
                .replace("<status-type-to-replace>", CredentialStatus.CREDENTIAL_TYPE)
                .replace("<status-purpose-to-replace>", CredentialStatus.STATUS_PURPOSE)
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithIssuerConflict)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("The issuer of the given credential " +
                        "http://example.edu/credentials/3735 is not the issuer of the StatusListCredential"))
            }

            // clean up created wallet
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }

        }
    }

}
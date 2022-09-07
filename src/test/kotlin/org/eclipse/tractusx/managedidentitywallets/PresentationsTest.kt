/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
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
import org.eclipse.tractusx.managedidentitywallets.models.WalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import java.io.File
import kotlin.test.*

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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
        }) {
            // programmatically add a wallet
            runBlocking {
                val walletDto =  EnvironmentTestSetup
                    .walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
                SingletonTestData.baseWalletVerKey = walletDto.verKey!!
                SingletonTestData.baseWalletDID = walletDto.did
            }

            val networkId = EnvironmentTestSetup.NETWORK_ID
            val invalidDID = SingletonTestData.baseWalletDID
                .replace("did:indy:$networkId", "did:indy:$networkId WRONG")
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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
        }) {
            // programmatically add a wallet
            runBlocking {
                val walletDto =  EnvironmentTestSetup
                    .walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
                SingletonTestData.baseWalletVerKey = walletDto.verKey!!
                SingletonTestData.baseWalletDID = walletDto.did
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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
        }) {
            // programmatically add base wallet
            runBlocking {
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "base"))
            }

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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
        }) {
            // programmatically add base wallet
            runBlocking {
                val walletDto = EnvironmentTestSetup.walletService.createWallet(
                    WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "base")
                )
                SingletonTestData.baseWalletDID = walletDto.did
                SingletonTestData.baseWalletVerKey = walletDto.verKey!!
                SingletonTestData.credentialIndex = 3
                SingletonTestData.revocationListName = walletDto.revocationListName!!
            }

            val verifiablePresentation = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "${SingletonTestData.baseWalletDID}",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "StatusList2021Entry",
                                "statusPurpose": "revocation",
                                "statusListIndex": "${SingletonTestData.credentialIndex}",
                                "statusListCredential": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        },
                        {
                            "id": "http://example.edu/credentials/3333",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D",
                            "issuanceDate": "2021-06-16T18:56:59Z",
                            "expirationDate": "2027-06-17T18:56:59Z",
                            "credentialSubject": {
                                "givenName": "TestAfterQuestion",
                                "familyName": "Student",
                                "degree": {
                                    "type": "Master1",
                                    "degreeType": "Undergraduate2",
                                    "name": "Master of Test1"
                                },
                                "college": "Test2",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:16:45Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..6oIPVm3ealRVzpgiFKItyIzVWlNUT150fbh9OcBElj9FvaICAd-wc1yzrwka3ns1SmrPFsWIIe0wC1rJQLISBA"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()


            SingletonTestData.encodedList = EnvironmentTestSetup.NONE_REVOKED_ENCODED_LIST
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentation)
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
                setBody(verifiablePresentation)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("The credential http://example.edu/credentials/3735 has been revoked!"))
            }

            val verifiablePresentationWithWrongStatusType = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "${SingletonTestData.baseWalletDID}",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "WrongType",
                                "statusPurpose": "revocation",
                                "statusListIndex": "${SingletonTestData.credentialIndex}",
                                "statusListCredential": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentationWithWrongStatusType)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid credential status 'Type'"))
            }

            val verifiablePresentationWithWrongStatusPurpose = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "${SingletonTestData.baseWalletDID}",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "StatusList2021Entry",
                                "statusPurpose": "wrong-purpose",
                                "statusListIndex": "${SingletonTestData.credentialIndex}",
                                "statusListCredential": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentationWithWrongStatusPurpose)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusPurpose'"))
            }

            val verifiablePresentationWithEmptyStatusListIndex = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "${SingletonTestData.baseWalletDID}",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "StatusList2021Entry",
                                "statusPurpose": "revocation",
                                "statusListIndex": " ",
                                "statusListCredential": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentationWithEmptyStatusListIndex)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListIndex'"))
            }

            val verifiablePresentationWithEmptyStatusListUrl = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "${SingletonTestData.baseWalletDID}",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "StatusList2021Entry",
                                "statusPurpose": "revocation",
                                "statusListIndex": "3",
                                "statusListCredential": "   "
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentationWithEmptyStatusListUrl)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListCredential'"))
            }

            val verifiablePresentationWithIssuerConflict = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3735",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9",
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
                                "college": "Test",
                                "id": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "credentialStatus": {
                                "id": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                                "type": "StatusList2021Entry",
                                "statusPurpose": "revocation",
                                "statusListIndex": "${SingletonTestData.credentialIndex}",
                                "statusListCredential": "http://localhost:8080/api/credentials/status/${SingletonTestData.revocationListName}"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "${SingletonTestData.baseWalletDID}#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(verifiablePresentationWithIssuerConflict)
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
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
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
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

    @Test
    fun testIssuePresentation() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
        }) {
            // programmatically add a wallet
            val walletDto: WalletDto
            runBlocking {
                walletDto =  EnvironmentTestSetup
                    .walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
            }

            val networkId = EnvironmentTestSetup.NETWORK_ID
            val invalidDID = walletDto.did.replace("did:indy:$networkId", "did:indy:$networkId WRONG")
            val verifiablePresentationRequestWithInvalidDIDs = VerifiablePresentationRequestDto(
                holderIdentifier = walletDto.did,
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
                        credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
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

            SingletonTestData.baseWalletVerKey = walletDto.verKey!!
            SingletonTestData.baseWalletDID = walletDto.did
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
            SingletonTestData.baseWalletVerKey = ""
            SingletonTestData.baseWalletDID = ""

            val verifiablePresentationRequest = VerifiablePresentationRequestDto(
                holderIdentifier = walletDto.did,
                verifiableCredentials = listOf(
                    VerifiableCredentialDto(
                        context = listOf(
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                        ),
                        id = "http://example.edu/credentials/333",
                        type = listOf("University-Degree-Credential, VerifiableCredential"),
                        issuer = walletDto.did,
                        issuanceDate = "2019-06-16T18:56:59Z",
                        expirationDate = "2999-06-17T18:56:59Z",
                        credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                        proof = LdProofDto(
                            type = "Ed25519Signature2018",
                            created = "2021-11-17T22:20:27Z",
                            proofPurpose = "assertionMethod",
                            verificationMethod = "${walletDto.did}#key-1",
                            jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                        )
                    )
                )
            )

            SingletonTestData.baseWalletVerKey = walletDto.verKey!!
            SingletonTestData.baseWalletDID = walletDto.did
            val signedCred = Json.encodeToString(
                VerifiablePresentationDto.serializer(),
                VerifiablePresentationDto(
                    context = listOf(JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1),
                    type =  listOf("VerifiablePresentation"),
                    holder = walletDto.did,
                    verifiableCredential = listOf(
                        VerifiableCredentialDto(
                            context = listOf(
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                            ),
                            id = "http://example.edu/credentials/3732",
                            type = listOf("University-Degree-Credential, VerifiableCredential"),
                            issuer = walletDto.did,
                            issuanceDate = "2019-06-16T18:56:59Z",
                            expirationDate = "2999-06-17T18:56:59Z",
                            credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                            proof = LdProofDto(
                                type = "Ed25519Signature2018",
                                created = "2021-11-17T22:20:27Z",
                                proofPurpose = "assertionMethod",
                                verificationMethod = "${walletDto.did}#key-1",
                                jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                            )
                        )
                    ),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${walletDto.did}#key-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )

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

            // Ignore credential validation
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

            SingletonTestData.baseWalletVerKey = ""
            SingletonTestData.baseWalletDID = ""
            SingletonTestData.signCredentialResponse = ""
            SingletonTestData.isValidVerifiableCredential = true

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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
        }) {
            // programmatically add base wallet and an additional one
            runBlocking {
                EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "base"))
            }

            val validVP = """
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
                            "issuer": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D",
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
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        },
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

            val vpWithInvalidDID = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:local:indy:test:AA5EEDcn8yTfMobaTcabj9",
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
                            "issuer": "did:local:indy:test:LCNSw1JxSTDw7EpR1UMG7D",
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
                                "id": "did:local:indy:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:local:indy:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        },
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
                            "issuer": "did:local:indy:test:LCNSw1JxSTDw7EpR1UMG7D",
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
                                "id": "did:local:indy:test:AA5EEDcn8yTfMobaTcabj9"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:16:45Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:local:indy:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..6oIPVm3ealRVzpgiFKItyIzVWlNUT150fbh9OcBElj9FvaICAd-wc1yzrwka3ns1SmrPFsWIIe0wC1rJQLISBA"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-12T12:28:44Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:local:indy:test:AA5EEDcn8yTfMobaTcabj9#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..FYkZonVoXojBcwC3yWvhiyBh4uR0hNZR1qyu5cZS5_PXiB8BEyKUolWzqBAX_u7bbKD5QGqbTECs9qLyD63wAg"
                    }
                }
            """.trimIndent()
            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithInvalidDID)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
            }


            val vpWithoutProof = """
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
                            "issuer": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D",
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
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-12T12:13:16Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..0_1pSjyxk4MCPkaatFlv78rTiE6JkI4iXM9QEOPwIGwLiyORkkKPe6TwaHoVvuarouC7ozpGZxWEGmVRqfiWDg"
                            }
                        }
                    ]
                }
            """.trimIndent()

            handleRequest(HttpMethod.Post, "/api/presentations/validation") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vpWithoutProof)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("Cannot verify verifiable presentation due to missing proof"))
            }

            val vpWithOutdatedVC = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "0c96720a-734d-41ea-89ca-92b4f8ba2fa8",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP",
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
                            "issuer": "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb",
                            "issuanceDate": "2021-06-16T18:56:59Z",
                            "expirationDate": "2021-06-17T18:56:59Z",
                            "credentialSubject": {
                                "givenName": "TestAfterQuestion",
                                "familyName": "Student",
                                "degree": {
                                    "type": "Master1",
                                    "degreeType": "Undergraduate2",
                                    "name": "Master of Test11"
                                },
                                "college": "Test2",
                                "id": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-13T14:18:56Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..qFl7sQ9-PUQwz7KV0ONn89AEDpx3DkUO_1LDYBHvdbw2FlPi_XM51pvh_6tx4fLwyMlZEp3VdAbxyRR-AdZWDw"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-13T14:19:32Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..lAbqnkVHOzt5GGuTebAgqBdt0p5vZvn7Z4dIarKPW3_BCSv9ATDzegkjzqOM3B91WP7flp93fgqmq5T-bT9YBw"
                    }
                }
            """.trimIndent()

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

            val vpWithFutureVC = """
                 {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "7aed00f7-8e04-4093-b467-9bd084b42086",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP",
                    "verifiableCredential": [
                        {
                            "id": "http://example.edu/credentials/3888",
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "type": [
                                "University-Degree-Credential",
                                "VerifiableCredential"
                            ],
                            "issuer": "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb",
                            "issuanceDate": "2999-06-16T18:56:59Z",
                            "expirationDate": "2999-06-17T18:56:59Z",
                            "credentialSubject": {
                                "givenName": "TestAfterQuestion",
                                "familyName": "Student",
                                "degree": {
                                    "type": "Master1",
                                    "degreeType": "Undergraduate2",
                                    "name": "Master of Test11"
                                },
                                "college": "Test2",
                                "id": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP"
                            },
                            "proof": {
                                "type": "Ed25519Signature2018",
                                "created": "2022-07-21T13:17:21Z",
                                "proofPurpose": "assertionMethod",
                                "verificationMethod": "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb#key-1",
                                "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..CvGRIw0aqQrXsXy1n3ChGfN1xs0Y56eiwS3spTlf_Ph4l5OQSFKId7SKNxBpFfI4GaQMKi8ajDVXvaIdT-N0DA"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-21T13:18:07Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..pnipnhAJ34b9k8kBpRJfEAOdbiaSZK38TAJveSYyoBrKAMhF3DAJ_b0pChHvgghzy9QiAsal5ZFkl5fakIGwAg"
                    }
                }
            """.trimIndent()

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

            val vpWithVcWithoutProof = """
                {
                    "@context": [
                        "https://www.w3.org/2018/credentials/v1"
                    ],
                    "id": "d312945e-826e-49cc-9baa-3c78d090745b",
                    "type": [
                        "VerifiablePresentation"
                    ],
                    "holder": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP",
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
                            "issuer": "did:indy:local:test:M6Mis1fZKuhEw71GNY3TAb",
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
                                "id": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP"
                            }
                        }
                    ],
                    "proof": {
                        "type": "Ed25519Signature2018",
                        "created": "2022-07-13T14:47:36Z",
                        "proofPurpose": "assertionMethod",
                        "verificationMethod": "did:indy:local:test:YHXZLLSLnKxz5D2HQaKXcP#key-1",
                        "jws": "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..fGJqT596Y9696mw97DVFkNZsuXU5xO-VCZWkEysOaeljl6loRZkQAVGmyzfZK4ZImcLKMFwHfgLv1E-Xxze7Bw"
                    }
                }
            """.trimIndent()

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

            val vpWithoutHolder = """
            {
                "@context": [
                    "https://www.w3.org/2018/credentials/v1"
                ],
                "id": "73e9e2f1-c0f9-4453-9619-d26244c83f15",
                "type": [
                    "VerifiablePresentation"
                ],
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
                        "issuer": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D",
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
                        "proof": {
                            "type": "Ed25519Signature2018",
                            "created": "2022-07-12T12:13:16Z",
                            "proofPurpose": "assertionMethod",
                            "verificationMethod": "did:indy:local:test:LCNSw1JxSTDw7EpR1UMG7D#key-1",
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
            SingletonTestData.isValidVerifiableCredential = true

            // clean up created wallets
            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN) // base wallet
                assertEquals(0, EnvironmentTestSetup.walletService.getAll().size)
            }

        }
    }

}
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
import org.eclipse.tractusx.managedidentitywallets.models.StoreVerifiableCredentialParameter
import org.eclipse.tractusx.managedidentitywallets.models.WalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.*
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import java.io.File
import kotlin.test.*

@kotlinx.serialization.ExperimentalSerializationApi
class CredentialsTest {

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
    fun testGetAndStoreVerifiableCredentials() {
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
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Catena-X-Wallet"
                )
            }

            var credentials = EnvironmentTestSetup.walletService.getCredentials(
                EnvironmentTestSetup.DEFAULT_BPN, EnvironmentTestSetup.DEFAULT_BPN,"RANDOM_TYPE","123-456"
            )
            assertTrue { credentials.isEmpty() }
            credentials = EnvironmentTestSetup.walletService.getCredentials(
                null,null,null,null)
            assertTrue { credentials.isEmpty() }

            //TODO replace issuerDid inside vcWithReplaceableSubjectId when did indy method is supported by AcaPy
            val vcAsString: String = File("./src/test/resources/credentials-test-data/vcWithReplaceableSubjectId.json")
                .readText(Charsets.UTF_8).replace("<subject-id-to-replace>", EnvironmentTestSetup.DEFAULT_DID)
            val storeVerifiableCredentialParameter = StoreVerifiableCredentialParameter(EnvironmentTestSetup.DEFAULT_BPN)
            assertEquals(EnvironmentTestSetup.DEFAULT_BPN, storeVerifiableCredentialParameter.identifier)

            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vcAsString)
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            val vcWithWrongSubjectAsString: String = File("./src/test/resources/credentials-test-data/vcWithReplaceableSubjectId.json")
                .readText(Charsets.UTF_8).replace("<subject-id-to-replace>",
                    "${SingletonTestData.getDidMethodPrefixWithNetworkIdentifier()}NotEqualWalletDID")
            handleRequest(HttpMethod.Post, "/api/wallets/${EnvironmentTestSetup.DEFAULT_BPN}/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vcWithWrongSubjectAsString)
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }

            handleRequest(HttpMethod.Get, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.VIEW_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(vcAsString)
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            credentials = EnvironmentTestSetup.walletService.getCredentials(
                null, EnvironmentTestSetup.DEFAULT_BPN,null,null)
            assertTrue { credentials.size == 1 }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssueCredential() {
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
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Catena-X-Wallet"
                )
                SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
            }
            val verifiableCredentialRequest = VerifiableCredentialRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                holderIdentifier = SingletonTestData.baseWalletDID,
                isRevocable = true
            )

            val signedCred = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = SingletonTestData.baseWalletDID,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
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
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""

            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequest,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // No Holder identifier , only subject ID
            val verifiableCredentialRequestNoHolderButWithSubjectId = VerifiableCredentialRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University", "id" to SingletonTestData.baseWalletDID),
            )
            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequestNoHolderButWithSubjectId,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // The Holder identifier is not a managed wallet
            val verifiableCredentialRequestWithRandomHolder = VerifiableCredentialRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                holderIdentifier = "Random-Value"
            )
            val signedCredWithRandomHolder = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = SingletonTestData.baseWalletDID,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to "Random-Value"),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${SingletonTestData.baseWalletDID}#key-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCredWithRandomHolder }"""
            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequestWithRandomHolder,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // No Holder identifier and no subject ID
            val signedCredWithoutSubjectId = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = SingletonTestData.baseWalletDID,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University"),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${SingletonTestData.baseWalletDID}#keys-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCredWithoutSubjectId }"""
            val verifiableCredentialRequestNoHolderNoSubjectId = VerifiableCredentialRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
            )
            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequestNoHolderNoSubjectId,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequest,
                    ))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // change did of test wallet
            val originalDID = SingletonTestData.baseWalletDID
            val replacedDID = SingletonTestData.baseWalletDID.replace(
                Services.utilsService.getDidMethodPrefixWithNetworkIdentifier(),
                Services.utilsService.getOldDidMethodPrefixWithNetworkIdentifier()
            )
            EnvironmentTestSetup.replaceWalletDid(originalDID, replacedDID)
            SingletonTestData.baseWalletDID = replacedDID

            // try to issue a credential
            val signedCredWithoutSubjectIdReplacedDid = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = SingletonTestData.baseWalletDID,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University"),
                    proof = LdProofDto(
                        type = "Ed25519Signature2018",
                        created = "2021-11-17T22:20:27Z",
                        proofPurpose = "assertionMethod",
                        verificationMethod = "${SingletonTestData.baseWalletDID}#keys-1",
                        jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                    )
                )
            )
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCredWithoutSubjectIdReplacedDid }"""
            val verifiableCredentialRequestNoHolderNoSubjectIdReplacedDid = VerifiableCredentialRequestDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuerIdentifier = SingletonTestData.baseWalletDID,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
            )
            handleRequest(HttpMethod.Post, "/api/credentials") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestDto.serializer(),
                        verifiableCredentialRequestNoHolderNoSubjectIdReplacedDid,
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            // change it back
            EnvironmentTestSetup.replaceWalletDid(SingletonTestData.baseWalletDID, originalDID)
            SingletonTestData.baseWalletDID = originalDID

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssueCredentialsByCatenaXWallet() {
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
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Catena-X-Wallet"
                )
                SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
                SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
            }
            val verifiableCredentialRequest = VerifiableCredentialRequestWithoutIssuerDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                holderIdentifier = SingletonTestData.baseWalletDID
            )

            val signedCred = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = SingletonTestData.baseWalletDID,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
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
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""

            handleRequest(HttpMethod.Post, "/api/credentials/issuer") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestWithoutIssuerDto.serializer(),
                    verifiableCredentialRequest))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }
            val verifiableCredentialRequestIrrevocable = VerifiableCredentialRequestWithoutIssuerDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University"),
                holderIdentifier = SingletonTestData.baseWalletDID
            )
            handleRequest(HttpMethod.Post, "/api/credentials/issuer") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    Json.encodeToString(
                        VerifiableCredentialRequestWithoutIssuerDto.serializer(),
                        verifiableCredentialRequestIrrevocable))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssueAndRevokeCredential() {
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
                    EnvironmentTestSetup.DEFAULT_BPN,
                    EnvironmentTestSetup.DEFAULT_DID,
                    EnvironmentTestSetup.DEFAULT_VERKEY,
                    "Catena-X-Wallet"
                )
            }
            val walletDto = EnvironmentTestSetup.walletService.getWallet(EnvironmentTestSetup.DEFAULT_BPN)
            SingletonTestData.baseWalletVerKey = EnvironmentTestSetup.DEFAULT_VERKEY
            SingletonTestData.baseWalletDID = EnvironmentTestSetup.DEFAULT_DID
            SingletonTestData.revocationListName = walletDto.revocationListName!!
            SingletonTestData.credentialIndex = 0
            val signedCred = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "StatusList2021Entry",
                        statusPurpose = "revocation",
                        index = SingletonTestData.credentialIndex.toString(),
                        listUrl = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}"
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
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""
            SingletonTestData.isValidVerifiableCredential = true
            SingletonTestData.credentialIndex = 1
            // revoke credential
            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCred)
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            val signedIrrevocableCredential = VerifiableCredentialDto(
                context = listOf(
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                    JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                ),
                id = "http://example.edu/credentials/3732",
                type = listOf("University-Degree-Credential, VerifiableCredential"),
                issuer = walletDto.did,
                issuanceDate = "2019-06-16T18:56:59Z",
                expirationDate = "2019-06-17T18:56:59Z",
                credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                proof = LdProofDto(
                    type = "Ed25519Signature2018",
                    created = "2021-11-17T22:20:27Z",
                    proofPurpose = "assertionMethod",
                    verificationMethod = "${walletDto.did}#key-1",
                    jws = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                )
            )

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(VerifiableCredentialDto.serializer(), signedIrrevocableCredential))
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("The given verifiable credential is not revocable!"))
            }

            val signedCredWithInvalidStatusType = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "Wrong-status-type",
                        statusPurpose = "revocation",
                        index = "-1",
                        listUrl = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}"
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

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCredWithInvalidStatusType)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid credential status 'Type'"))
            }

            val signedCredWithInvalidStatusPurpose = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "StatusList2021Entry",
                        statusPurpose = "wrong-purpose",
                        index = "1",
                        listUrl = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}"
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

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCredWithInvalidStatusPurpose)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusPurpose'"))
            }

            val signedCredWithInvalidIndex = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "StatusList2021Entry",
                        statusPurpose = "revocation",
                        index = "-1",
                        listUrl = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}"
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

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCredWithInvalidIndex)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListIndex'"))
            }

            val signedCredWithInvalidStatusListUrl = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "StatusList2021Entry",
                        statusPurpose = "revocation",
                        index = "3",
                        listUrl = "  "
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

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCredWithInvalidStatusListUrl)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("has invalid 'statusListCredential'"))
            }

            val signedCredWithInvalidStatusListUrlWithoutCredentialId = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    type = listOf("University-Degree-Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    expirationDate = "2019-06-17T18:56:59Z",
                    credentialSubject = mapOf("college" to "Test-University", "id" to walletDto.did),
                    credentialStatus = CredentialStatus(
                        statusId = "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#${SingletonTestData.credentialIndex}",
                        credentialType = "StatusList2021Entry",
                        statusPurpose = "revocation",
                        index = "  ",
                        listUrl = "  "
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

            handleRequest(HttpMethod.Post, "/api/credentials/revocations") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(signedCredWithInvalidStatusListUrlWithoutCredentialId)
            }.apply {
                assertEquals(HttpStatusCode.UnprocessableEntity, response.status())
                assertTrue(response.content!!.contains("Credential with Id null has invalid 'statusListIndex'"))
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssueStatusListCredential() {
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
        }) {
            // programmatically add a wallet
            val walletDto: WalletDto
            runBlocking {
                walletDto =  EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
            }

            SingletonTestData.baseWalletVerKey = walletDto.verKey!!
            SingletonTestData.baseWalletDID = walletDto.did
            SingletonTestData.revocationListName = walletDto.revocationListName!!
            SingletonTestData.credentialIndex = 0

            val signedCred = Json.encodeToString(
                VerifiableCredentialDto.serializer(),
                VerifiableCredentialDto(
                    context = listOf(
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1,
                        JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_EXAMPLES_V1
                    ),
                    id = "http://example.edu/credentials/3732",
                    type = listOf("StatusList2021Credential, VerifiableCredential"),
                    issuer = walletDto.did,
                    issuanceDate = "2019-06-16T18:56:59Z",
                    credentialSubject = mapOf(
                        "id" to "https://example.com/status/${Services.utilsService.getIdentifierOfDid(walletDto.did)}#list",
                        "type" to "StatusList2021",
                        "statusPurpose" to "revocation",
                        "encodedList" to "H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
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
            SingletonTestData.signCredentialResponse = """{ "signed_doc": $signedCred }"""
            SingletonTestData.isValidVerifiableCredential = true
            val listCredentialRequestData = ListCredentialRequestData(
                listId = "urn:uuid:93731387-dec1-4bf6-8087-d5210f661421",
                subject = ListCredentialSubject (
                    credentialId = "https://example.com/status/3#list",
                    credentialType = "StatusList2021",
                    statusPurpose = "revocation",
                    encodedList = "H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
                )
            )

            handleRequest(
                HttpMethod.Post, "/list-credential/${Services.utilsService.getIdentifierOfDid(walletDto.did)}/issue") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(ListCredentialRequestData.serializer(), listCredentialRequestData))
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testGetStatusListCredential() {
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
            val walletDto: WalletDto
            runBlocking {
                walletDto =  EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
            }

            SingletonTestData.baseWalletVerKey = walletDto.verKey!!
            SingletonTestData.baseWalletDID = walletDto.did
            SingletonTestData.revocationListName = walletDto.revocationListName!!

            handleRequest(
                HttpMethod.Get, "/api/credentials/status/${walletDto.revocationListName}") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val credential = Json.decodeFromString(VerifiableCredentialDto.serializer(), response.content!!)
                assertEquals(
                    "https://example.com/api/credentials/status/${SingletonTestData.revocationListName}#list",
                    credential.credentialSubject["id"]
                )
            }

            // without listName
            handleRequest(
                HttpMethod.Get, "/api/credentials/status/") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }

    @Test
    fun testIssueAndUpdateStatusListCredential() {
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
        }) {
            // programmatically add a wallet
            var walletDto: WalletDto
            runBlocking {
                walletDto = EnvironmentTestSetup.walletService.createWallet(WalletCreateDto(EnvironmentTestSetup.DEFAULT_BPN, "name_default"))
            }

            handleRequest(
                HttpMethod.Post, "/api/credentials/revocations/statusListCredentialRefresh") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            handleRequest(
                HttpMethod.Post, "/api/credentials/revocations/statusListCredentialRefresh?identifier=${walletDto.did}&force=false") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            handleRequest(
                HttpMethod.Post, "/api/credentials/revocations/statusListCredentialRefresh?identifier=${walletDto.did}&force=true") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }

            handleRequest(
                HttpMethod.Get, "/api/credentials/status/listName=${walletDto.revocationListName}") {
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            runBlocking {
                EnvironmentTestSetup.walletService.deleteWallet(EnvironmentTestSetup.DEFAULT_BPN)
            }
        }
    }
}
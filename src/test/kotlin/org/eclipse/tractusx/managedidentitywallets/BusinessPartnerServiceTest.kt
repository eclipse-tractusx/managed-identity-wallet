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

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toImmutableList
import org.eclipse.tractusx.managedidentitywallets.models.BPDMConfig
import org.eclipse.tractusx.managedidentitywallets.models.LegalFormDto
import org.eclipse.tractusx.managedidentitywallets.models.NameResponse
import org.eclipse.tractusx.managedidentitywallets.models.TypeKeyNameDto
import org.eclipse.tractusx.managedidentitywallets.models.TypeKeyNameUrlDto
import org.eclipse.tractusx.managedidentitywallets.models.TypeNameUrlDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdContexts
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdTypes
import org.eclipse.tractusx.managedidentitywallets.models.ssi.LdProofDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.Rfc23State
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.eclipse.tractusx.managedidentitywallets.plugins.configurePersistence
import org.eclipse.tractusx.managedidentitywallets.services.AcaPyService
import org.eclipse.tractusx.managedidentitywallets.services.AcaPyWalletServiceImpl
import org.eclipse.tractusx.managedidentitywallets.services.BusinessPartnerDataServiceImpl
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService
import org.eclipse.tractusx.managedidentitywallets.services.WebhookServiceImpl
import org.hyperledger.acy_py.generated.model.AttachDecorator
import org.hyperledger.acy_py.generated.model.AttachDecoratorData
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredOffer
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.MockitoKotlinException
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@kotlinx.serialization.ExperimentalSerializationApi
class BusinessPartnerServiceTest {

    private val connectionId = UUID.randomUUID().toString()

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

    // Is the issuer wallet Without walletKey and walletToken
    private val baseWallet = WalletExtendedData(
        id = null,
        name = issuerWallet.name,
        did = issuerWallet.did,
        bpn = issuerWallet.bpn,
        walletId = "",
        walletKey = null,
        walletToken = null,
        revocationListName = null,
        pendingMembershipIssuance = false
    )

    private var holderWallet = WalletExtendedData(
        id = 2,
        name = "holder wallet",
        bpn = "BPNL000000000001",
        did = "did:sov:holder1",
        walletId = "walletId",
        walletKey = "walletKey",
        walletToken = "walletToken",
        revocationListName = null,
        pendingMembershipIssuance = true
    )

    private var selfManagedWallet = WalletExtendedData(
        id = 2,
        name = "self managed wallet",
        bpn = "BPNL000000000001",
        did = "did:sov:selfmanaged2",
        walletId = null,
        walletKey = null,
        walletToken = null,
        revocationListName = null,
        pendingMembershipIssuance = true
    )

    private val bpdmConfig = BPDMConfig(
        url = "test",
        tokenUrl = "tokenURL",
        clientId = "test",
        clientSecret = "test",
        grantType = "test",
        scope = "test",
    )

    private val bpnSubject = mapOf(
        "id" to holderWallet.did,
        "type" to JsonLdTypes.NAME_TYPE,
        "bpn" to holderWallet.bpn
    )

    private val membershipSubject = mapOf(
        "id" to holderWallet.did,
        "type" to listOf(JsonLdTypes.MEMBERSHIP_TYPE),
        "memberOf" to "Catena-X",
        "status" to "Active",
        "startTime" to "currentDateAsString",
    )

    private val nameSubject = mapOf(
        "id" to holderWallet.did,
        "type" to JsonLdTypes.NAME_TYPE,
        "data" to mapOf(
            "value" to "German Car Company",
            "shortName" to "GCC",
            "type" to mapOf(
                "technicalKey" to "REGISTERED",
                "name" to "The main name under which a business is officially registered in a country's business register.",
                "url" to ""
            ),
            "language" to mapOf(
                "technicalKey" to "undefined",
                "name" to "Undefined"
            )
        )
    )

    private val newNameSubject = mapOf(
        "id" to holderWallet.did,
        "type" to JsonLdTypes.NAME_TYPE,
        "data" to mapOf(
            "value" to "NEW German Car Company",
            "shortName" to "GCC",
            "type" to mapOf(
                "technicalKey" to "REGISTERED",
                "name" to "The main name under which a business is officially registered in a country's business register.",
                "url" to ""
            ),
            "language" to mapOf(
                "technicalKey" to "undefined",
                "name" to "Undefined"
            )
        )
    )

    private val nameResponseData = NameResponse(
        value = "German Car Company",
        shortName = "GCC",
        type = TypeKeyNameUrlDto(
            technicalKey = "REGISTERED",
            name = "The main name under which a business is officially registered in a country's business register.",
            url = ""
        ),
        language = TypeKeyNameDto(
            technicalKey = "undefined",
            name = "Undefined"
        )
    )

    private val legalFormSubject = mapOf(
        "id" to selfManagedWallet.did,
        "type" to JsonLdTypes.LEGAL_FORM_TYPE,
        "data" to mapOf(
            "technicalKey" to "DE_AG",
            "name" to "Aktiengesellschaft",
            "url" to "",
            "mainAbbreviation" to "AG",
            "categories" to listOf(
                mapOf(
                    "name" to "AG",
                    "url" to ""
                )
            ),
            "language" to mapOf(
                "technicalKey" to "undefined",
                "name" to "Undefined"
            )
        )
    )

    private val legalFormdata = LegalFormDto(
        technicalKey = "DE_AG",
        name = "Aktiengesellschaft",
        url = "",
        mainAbbreviation = "AG",
        categories = listOf(
            TypeNameUrlDto(
                name = "AG",
                url = ""
            )
        ),
        language = TypeKeyNameDto(
            technicalKey = "undefined",
            name = "Undefined"
        )
    )

    private val accessToken =
        """{
            "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJUUnR0Wk1MT2Nia2wzeWtjOWtpcmR0c2ZJdk9tRHp4WXFQQTJ2RXdjZ3pVIn0.eyJleHAiOjE2NjkxNTAxODksImlhdCI6MTY2OTEzMjE4OSwianRpIjoiNGNlYTY3ZGMtYzdlOC00ODAxLWJhOGItYTU3ZmNkYjQ4Mjg3IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgxL2F1dGgvcmVhbG1zL2NhdGVuYXgiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiODc5MmI0NTgtYjg0OC00YWFjLThmNDEtMTAyZmQ2NTg4YTk0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiTWFuYWdlZElkZW50aXR5V2FsbGV0cyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWNhdGVuYXgiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Ik1hbmFnZWRJZGVudGl0eVdhbGxldHMiOnsicm9sZXMiOlsiZGVsZXRlX3dhbGxldCIsImRlbGV0ZV93YWxsZXRzIiwidXBkYXRlX3dhbGxldHMiLCJ2aWV3X3dhbGxldHMiLCJhZGRfd2FsbGV0IiwidXBkYXRlX3dhbGxldCIsInZpZXdfd2FsbGV0IiwiYWRkX3dhbGxldHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJCUE4iOiJCUE5MMDAwMDAwMDAxIiwiY2xpZW50SWQiOiJNYW5hZ2VkSWRlbnRpdHlXYWxsZXRzIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTcyLjE4LjAuMSIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1tYW5hZ2VkaWRlbnRpdHl3YWxsZXRzIiwiY2xpZW50QWRkcmVzcyI6IjE3Mi4xOC4wLjEifQ.K8KLwV7fHsM7d_5XzmyWUkS7VrDFWfM3TOwGzRrN4lARH6BAwWY-_7_G3xmU0EWpQfaluCxDxFrhDNUtyEHvdu4wxI19UyxXRPJ3O6qNFgm32-j5O32hav40zTF8ljMhgNW-muV2pBCTipvXQDRNid6C1jJ1PMus5w5iyTF0SZ7Vxf7yEiJD2zWeZi5_PqKgBljaFbrX68xNXxf-pJ9o33xBdvgCAaL5xX0saarHHdIsytXCheB9uajhhdA0-XWQRF4MaPIHV2y5D2_ttbMcXfmp4w9_CZCV5LDOc184U-JV8mBs1jOa9Yb4aJ4zBlVj32RQT-ayk_NRT6pq8EetDA",
            "expires_in": 18000,
            "refresh_expires_in": 0,
            "token_type": "Bearer",
            "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJUUnR0Wk1MT2Nia2wzeWtjOWtpcmR0c2ZJdk9tRHp4WXFQQTJ2RXdjZ3pVIn0.eyJleHAiOjE2NjkxNTAxODksImlhdCI6MTY2OTEzMjE4OSwiYXV0aF90aW1lIjowLCJqdGkiOiIxOGE2ZmM1MC00ZWE0LTRlYTgtYWUyYi1kN2U5NzliNWEyM2QiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODEvYXV0aC9yZWFsbXMvY2F0ZW5heCIsImF1ZCI6Ik1hbmFnZWRJZGVudGl0eVdhbGxldHMiLCJzdWIiOiI4NzkyYjQ1OC1iODQ4LTRhYWMtOGY0MS0xMDJmZDY1ODhhOTQiLCJ0eXAiOiJJRCIsImF6cCI6Ik1hbmFnZWRJZGVudGl0eVdhbGxldHMiLCJhdF9oYXNoIjoiczF1dUJ5Z0FBejJZUFByWGdGVmtjZyIsImFjciI6IjEiLCJjbGllbnRJZCI6Ik1hbmFnZWRJZGVudGl0eVdhbGxldHMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudEhvc3QiOiIxNzIuMTguMC4xIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LW1hbmFnZWRpZGVudGl0eXdhbGxldHMiLCJjbGllbnRBZGRyZXNzIjoiMTcyLjE4LjAuMSJ9.IWtW0kJesPQHymUu_SPGUCbuTqGAHhG4kqSuqwpsBJ2UM0R7rlif5WRrzIEjhWAfGjD-RWnYx50Yu21qpn2wEMaxkzPoZtvfST9dOZurKR6e35Z6z1FW3fEB4O5XTTruytxPpv2u0tVslI8i7NZcmVVSegg76tEJmFMf7_6lftia-tecvrwKenoVKe291Ej8opaLokBky48OAvLkKEVN4aNBOkYSQ8M1gabk4DHC7m8U3uVVP7MnghOFGgFN6Kgr5DnjUu-Ra8lhv72NEmFoVwLYUy2mzHxAwa6cJHFX5O4ek_AJjkyedWHXejAA8uP2zslhqp5l0GHLGiOr4_DIMA",
            "not-before-policy": 0,
            "scope": "openid email profile"
        }"""

    private val mockEngine = MockEngine { request ->
        if (request.url.toString().endsWith(bpdmConfig.tokenUrl)) {
            respondOk(
                content = accessToken
            )
        } else if (request.url.toString().endsWith("legal-addresses/search")) {
            respondOk(
                content = """[]""" // empty addresses
            )
        } else if (request.url.toString().endsWith("legal-entities/search")) {
            val legalEntityAsString: String = File("./src/test/resources/bpdm-test-data/legalEntityOnlyName.json")
                .readText(Charsets.UTF_8)
            respondOk(
                content = legalEntityAsString,
            )
        } else {
            fail("Unexpected Http request")
        }
    }

    private lateinit var walletRepo: WalletRepository
    private lateinit var connectionRepository: ConnectionRepository
    private lateinit var webhookRepository: WebhookRepository
    private lateinit var credentialRepository: CredentialRepository
    private lateinit var utilsService: UtilsService
    private lateinit var revocationService: IRevocationService
    private lateinit var webhookService: IWebhookService
    private lateinit var walletService: IWalletService
    private lateinit var bpdmService: IBusinessPartnerDataService

    @BeforeTest
    fun setup() {
        walletRepo = WalletRepository()
        connectionRepository = ConnectionRepository()
        webhookRepository = WebhookRepository()
        credentialRepository = CredentialRepository()
        utilsService = UtilsService("")
        revocationService = mock<IRevocationService>()
        val client = HttpClient(mockEngine) {
            expectSuccess = false
        }
        webhookService = WebhookServiceImpl(webhookRepository, client)
    }

    @Test
    fun testIssueAndStoreBaseWalletCredentialsAsync() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create Wallets, init services and mocks
                val acapyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acapyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acapyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acapyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val walletServiceSpy = spy(walletService)
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))
                val holderWalletDto = walletService.getWallet(holderWallet.did, false)
                val listOfCredentialIds = listOf(
                    "urn:uuid:93731387-dec1-4bf6-8087-d5210f771421",
                    "urn:uuid:93731387-dec1-4bf6-8087-d5210f771422",
                    "urn:uuid:93731387-dec1-4bf6-8087-d5210f771423",
                )
                // Mock BPN Cred
                val credentialBpnId = listOfCredentialIds[0]
                doReturn(
                    createVCDto(credentialBpnId, issuerWallet.did, JsonLdTypes.BPN_TYPE, bpnSubject)
                ).whenever(walletServiceSpy).issueBaseWalletCredential(any())
                // Test create bpn credential
                bpdmService.issueAndStoreBaseWalletCredentialsAsync(
                    holderWalletDto,
                    JsonLdTypes.BPN_TYPE,
                    null
                ).await()

                // Mock Membership Credential
                val credentialMembershipId = listOfCredentialIds[1]
                doReturn(
                    createVCDto(
                        credentialMembershipId,
                        issuerWallet.did,
                        JsonLdTypes.MEMBERSHIP_TYPE,
                        membershipSubject
                    )
                ).whenever(walletServiceSpy).issueBaseWalletCredential(any())
                // Test create membership credential
                bpdmService.issueAndStoreBaseWalletCredentialsAsync(
                    holderWalletDto,
                    JsonLdTypes.MEMBERSHIP_TYPE,
                    null
                ).await()

                // Mock Name Credential
                val credentialNameId = listOfCredentialIds[2]
                doReturn(
                    createVCDto(credentialNameId, issuerWallet.did, JsonLdTypes.NAME_TYPE, nameSubject)
                ).whenever(walletServiceSpy).issueBaseWalletCredential(any())

                // Test create name credential
                bpdmService.issueAndStoreBaseWalletCredentialsAsync(
                    holderWalletDto,
                    JsonLdTypes.NAME_TYPE,
                    nameResponseData
                ).await()

                // Test all 3 previously created credentials are stored correctly
                transaction {
                    val extractedWallet = walletServiceSpy.getWallet(holderWallet.did, true)
                    assertEquals(3, extractedWallet.vcs.size)
                }

                // Remove wallets and credentials
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(holderWallet.did)
                    listOfCredentialIds.forEach {
                        credentialRepository.deleteCredentialByCredentialId(it)
                    }
                }
            }
        }
    }

    @Test
    fun testIssueAndStoreBaseWalletCredentialsAsyncErrors() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create Wallets, init services and mocks
                val acaPyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acaPyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acaPyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acaPyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))
                val holderWalletDto = walletService.getWallet(holderWallet.did, false)
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )

                doThrow(
                    MockitoKotlinException("mocked-exception", null)
                ).doReturn(
                    VerifiableCredentialDto(
                        id = "id",
                        type = listOf(),
                        context = listOf(),
                        issuer = issuerWallet.did,
                        credentialSubject = mapOf("data" to "data"),
                        issuanceDate = "now"
                    )
                ).whenever(walletServiceSpy).issueBaseWalletCredential(any())

                // Test create credential
                val resultWithException = bpdmService.issueAndStoreBaseWalletCredentialsAsync(
                    holderWalletDto,
                    JsonLdTypes.NAME_TYPE,
                    nameResponseData
                ).await()

                val resultWithEmptyProofForIssuedCred = bpdmService.issueAndStoreBaseWalletCredentialsAsync(
                    holderWalletDto,
                    JsonLdTypes.NAME_TYPE,
                    nameResponseData
                ).await()

                assertEquals(false, resultWithException)
                assertEquals(false, resultWithEmptyProofForIssuedCred)

                // Test no credentials are created and stored due mocked exceptions
                transaction {
                    val extractedWallet = walletServiceSpy.getWallet(holderWallet.did, true)
                    assertEquals(0, extractedWallet.vcs.size)
                }

                // Remove wallets and credentials
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(holderWallet.did)
                }
            }
        }
    }

    @Test
    fun testIssueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create wallets and connection, init services and mocks

                addConnection(
                    connectionRepository,
                    Rfc23State.COMPLETED.toString(),
                    connectionId,
                    issuerWallet.did,
                    selfManagedWallet.did
                )
                val acapyServiceMocked = mock<AcaPyService>()
                whenever(acapyServiceMocked.getWalletAndAcaPyConfig()).thenReturn(
                    EnvironmentTestSetup.walletAcapyConfig
                )
                doNothing().whenever(acapyServiceMocked).subscribeBaseWalletForWebSocket()

                // Mock credential exchange offer
                val threadId = "thread-id"
                val v20CredentialExchange = createCredentialExchange(threadId)
                whenever(acapyServiceMocked.issuanceFlowCredentialSend(anyOrNull(), any()))
                    .doReturn(v20CredentialExchange)

                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acapyServiceMocked,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, selfManagedWallet))
                val selfManagedWalletDto = walletService.getWallet(selfManagedWallet.did, false)
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )

                // Mock LegalForm Credential
                val credentialLegalFormId = "urn:uuid:93731387-dec1-4bf6-8087-d5210f771333"
                doReturn(
                    createVCDto(
                        credentialLegalFormId,
                        issuerWallet.did,
                        JsonLdTypes.LEGAL_FORM_TYPE,
                        legalFormSubject
                    )
                ).whenever(walletServiceSpy).issueBaseWalletCredential(any())



                // Test `issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync`
                bpdmService.issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                    selfManagedWalletDto,
                    connectionId,
                    "webhook-url",
                    JsonLdTypes.LEGAL_FORM_TYPE,
                    legalFormdata
                ).await()
                transaction {
                    val webhook = webhookRepository.get(threadId)
                    assertEquals(CredentialExchangeState.OFFER_SENT.toString(), webhook.state)
                }

                // Remove wallets, webhook and connection
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(selfManagedWallet.did)
                    webhookRepository.deleteWebhook(threadId)
                    connectionRepository.deleteConnection(connectionId)
                }

            }
        }
    }

    @Test
    fun testPullDataAndUpdateBaseWalletCredentialsAsyncBpdmHttpRequest()  {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create Wallets, init Services and mocks
                val acaPyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acaPyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acaPyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acaPyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                var callCounter = 0
                val mockEngine = MockEngine {
                        when (callCounter) {
                            0 -> { // access Token
                                callCounter++
                                respond(
                                    content = ByteReadChannel(accessToken),
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                                )
                            }
                            1 -> { // get legal address
                                callCounter++
                                respondBadRequest()
                            }
                            2 -> { // get business partner data first try
                                callCounter++
                                respondBadRequest()
                            }
                            else -> {
                                fail("Unexpected Http request")
                            }
                        }
                }
                val client = HttpClient(mockEngine) {
                    expectSuccess = false
                }
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )
                val spyBpdmService = spy(bpdmService)

                // Test `pullDataAndUpdateBaseWalletCredentialsAsync` for a created Wallet
                // no credentials will be created because The HTTP.OK state is never reached due the mockEngine
                assertDoesNotThrow {
                    runBlocking {
                        spyBpdmService.pullDataAndUpdateBaseWalletCredentialsAsync(holderWallet.did).await()
                        verify(spyBpdmService, never()).issueAndStoreBaseWalletCredentialsAsync(
                            any(),
                            any(),
                            any()
                        )
                        verify(spyBpdmService, never()).issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                            any(),
                            any(),
                            any(),
                            any(),
                            any()
                        )
                        val credentials = credentialRepository.getCredentials(
                            null, holderWallet.did, null, null
                        )
                        assertEquals(0, credentials.size)
                    }
                }

                // Remove wallets
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(holderWallet.did)
                }
            }
        }
    }

    @Test
    fun testPullDataAndUpdateBaseWalletCredentialsAsync() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create Wallets, init Services and mocks
                val acaPyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acaPyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acaPyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acaPyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val client = HttpClient(mockEngine) {
                    expectSuccess = false
                }
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )
                val spyBpdmService = spy(bpdmService)
                doReturn(
                    CompletableDeferred(true)
                ).whenever(spyBpdmService).issueAndStoreBaseWalletCredentialsAsync(
                    any(),
                    any(),
                    any(),
                )

                // Test `pullDataAndUpdateBaseWalletCredentialsAsync` for a created Wallet
                assertDoesNotThrow {
                    runBlocking {
                        spyBpdmService.pullDataAndUpdateBaseWalletCredentialsAsync(holderWallet.did).await()
                    }
                }
                verify(spyBpdmService, never()).issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
                transaction {
                    val credentials = credentialRepository.getCredentials(
                        null, holderWallet.did, null, null
                    )
                    // The issue credential method `issueAndStoreBaseWalletCredentialsAsync`
                    //  is mocked to do nothing. Therefore, there is no new Credential
                    assertEquals(0, credentials.size)
                }

                // Remove wallets
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(holderWallet.did)
                }
            }
        }
    }

    @Test
    fun testPullDataAndUpdateBaseWalletCredentialsAsyncForSelfManagedWallet() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create wallet and connection, init services and mocks
                addConnection(
                    connectionRepository = connectionRepository,
                    state = Rfc23State.COMPLETED.toString(),
                    connectionId = connectionId,
                    myDid = issuerWallet.did,
                    theirDid = selfManagedWallet.did
                )
                val acaPyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acaPyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acaPyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acaPyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, selfManagedWallet))
                doReturn(
                    baseWallet
                ).whenever(walletServiceSpy).getBaseWallet()
                val client = HttpClient(mockEngine) {
                    expectSuccess = false
                }
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )
                val bpdmServiceSpy = spy(bpdmService)

                doReturn(
                    CompletableDeferred(true)
                ).whenever(bpdmServiceSpy).issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                    any(),
                    anyString(),
                    anyOrNull(),
                    any(),
                    any()
                )

                // Test `pullDataAndUpdateBaseWalletCredentialsAsync` for self-managed wallet
                assertDoesNotThrow {
                    runBlocking {
                        bpdmServiceSpy.pullDataAndUpdateBaseWalletCredentialsAsync(selfManagedWallet.did).await()
                    }
                }
                verify(bpdmServiceSpy, never()).issueAndStoreBaseWalletCredentialsAsync(
                    any(),
                    any(),
                    any()
                )
                transaction {
                    val credentials = credentialRepository.getCredentials(
                        null, holderWallet.did, null, null
                    )
                    // The issue credential method `issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync`
                    //  is mocked to do nothing. Therefore, there is no new Credential
                    assertEquals(0, credentials.size)
                }

                // Remove wallets and connection
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(selfManagedWallet.did)
                    connectionRepository.deleteConnection(connectionId)
                }
            }
        }
    }

    @Test
    fun testPullDataAndUpdateBaseWalletCredentialsAsyncWithRevoke() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
        }) {
            runBlocking {
                // Setup: Create wallets, init Services and mocks
                val acaPyServiceMock = mock<AcaPyService>()
                doNothing().whenever(acaPyServiceMock).subscribeBaseWalletForWebSocket()
                whenever(acaPyServiceMock.getWalletAndAcaPyConfig()).thenReturn(EnvironmentTestSetup.walletAcapyConfig)
                whenever(revocationService.registerList(any(), any())).thenReturn(UUID.randomUUID().toString())
                walletService = AcaPyWalletServiceImpl(
                    acaPyService = acaPyServiceMock,
                    walletRepository = walletRepo,
                    credentialRepository = credentialRepository,
                    utilsService = utilsService,
                    revocationService = revocationService,
                    webhookService = webhookService,
                    connectionRepository = connectionRepository
                )
                val walletServiceSpy = spy(walletService)
                addWallets(walletRepo, walletServiceSpy, listOf(issuerWallet, holderWallet))
                doAnswer { null }.whenever(walletServiceSpy).revokeVerifiableCredential(any())
                val client = HttpClient(mockEngine) {
                    expectSuccess = false
                }
                bpdmService = BusinessPartnerDataServiceImpl(
                    walletServiceSpy,
                    bpdmConfig,
                    client
                )
                val bpdmServiceSpy = spy(bpdmService)
                doReturn(
                    CompletableDeferred(true)
                ).whenever(bpdmServiceSpy).issueAndStoreBaseWalletCredentialsAsync(
                    any(),
                    any(),
                    any(),
                )
                // Store mocked credential in DB. This credential is not equal to the pulled data from Bpdm
                val credentialId = "urn:uuid:93731387-dec1-4bf6-8087-d5210f771421"
                transaction {
                    // not equal to the pulled data
                    walletService.storeCredential(
                        holderWallet.did,
                        IssuedVerifiableCredentialRequestDto(
                            id = credentialId,
                            context = listOf(
                                JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS,
                                JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1
                            ),
                            type = listOf(JsonLdTypes.NAME_TYPE, JsonLdTypes.CREDENTIAL_TYPE),
                            issuanceDate = "2021-06-16T18:56:59Z",
                            expirationDate = "2026-06-17T18:56:59Z",
                            issuer = issuerWallet.did,
                            credentialSubject = newNameSubject,
                            credentialStatus = null,
                            proof = LdProofDto(
                                type = "Ed25519Signature2018",
                                created = "2022-11-17T12:57:50Z",
                                proofPurpose = "assertionMethod",
                                verificationMethod = "${issuerWallet.did}#key-1",
                                jws = "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..Zo4X0KtpOd3OURpOlN6_lNxlZUyqNkpiopC5-JrajCPdQnYUE_ZM0v4GCi7Q8W6vy0Tme4gyos7SVxydDyBuAQ"
                            )
                        )
                    )
                }

                // Test `pullDataAndUpdateBaseWalletCredentialsAsync` with created Wallet
                // and existing credential to be revoked
                bpdmServiceSpy.pullDataAndUpdateBaseWalletCredentialsAsync(holderWallet.did).await()
                transaction {
                    val credentials = credentialRepository.getCredentials(
                        null, holderWallet.did, null, null
                    )
                    // The stored credential is deleted.
                    // The issue credential method is mocked to do nothing. Therefore, there is no new Credential
                    assertEquals(0, credentials.size)
                }

                // Remove wallets
                transaction {
                    walletRepo.deleteWallet(issuerWallet.did)
                    walletRepo.deleteWallet(holderWallet.did)
                }

            }
        }
    }

    private fun createVCDto(
        credentialNameId: String,
        issuerDid: String,
        type: String,
        subject: Map<String, Any>
    ) = VerifiableCredentialDto(
        id = credentialNameId,
        context = listOf(
            JsonLdContexts.JSONLD_CONTEXT_BPD_CREDENTIALS,
            JsonLdContexts.JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1
        ),
        type = listOf(type, JsonLdTypes.CREDENTIAL_TYPE),
        issuanceDate = "2021-06-16T18:56:59Z",
        expirationDate = "2026-06-17T18:56:59Z",
        issuer = issuerDid,
        credentialSubject = subject,
        credentialStatus = null,
        proof = LdProofDto(
            type = "Ed25519Signature2018",
            created = "2022-11-17T12:57:50Z",
            proofPurpose = "assertionMethod",
            verificationMethod = "$issuerDid#key-1",
            jws = "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..Zo4X0KtpOd3OURpOlN6_lNxlZUyqNkpiopC5-JrajCPdQnYUE_ZM0v4GCi7Q8W6vy0Tme4gyos7SVxydDyBuAQ"
        )
    )

    private fun createCredentialExchange(threadId: String): V20CredExRecord {
        val data = AttachDecoratorData()
        data.base64 = File("./src/test/resources/credentials-test-data/vcBase64.txt")
            .readText(Charsets.UTF_8)
        val dataDecorator = AttachDecorator()
        dataDecorator.data = data
        val credentialAttach = listOf(dataDecorator)
        val credOffer = V20CredOffer()
        credOffer.offersAttach = credentialAttach.toImmutableList()
        val v20CredentialExchange = V20CredExRecord()
        v20CredentialExchange.state = CredentialExchangeState.OFFER_SENT
        v20CredentialExchange.threadId = threadId
        v20CredentialExchange.credOffer = credOffer
        return v20CredentialExchange
    }

    private fun addWallets(walletRepo: WalletRepository, walletService: IWalletService, wallets: List<WalletExtendedData>) {
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

    private fun addConnection(
        connectionRepository: ConnectionRepository,
        state: String,
        connectionId: String,
        myDid: String,
        theirDid: String
    ) {
        transaction {
            connectionRepository.add(
                idOfConnection = connectionId,
                connectionTargetDid = theirDid,
                connectionOwnerDid = myDid,
                rfc23State = state
            )
        }
    }
}

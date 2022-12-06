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

package org.eclipse.tractusx.managedidentitywallets.services

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.Services
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.RegisterNymIdunionDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.RegisterNymPublicDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*
import org.hyperledger.acy_py.generated.model.V20CredRequestRequest
import org.hyperledger.acy_py.generated.model.V20CredStoreRequest
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.AriesWebSocketClient
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree
import org.hyperledger.aries.api.jsonld.ProofType
import org.hyperledger.aries.api.jsonld.VerifiableCredential
import java.util.*

class AcaPyService(
    private val acaPyConfig: WalletAndAcaPyConfig,
    private val utilsService: UtilsService,
    private val client: HttpClient
) : IAcaPyService {

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create()

    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        return WalletAndAcaPyConfig(
            apiAdminUrl = acaPyConfig.apiAdminUrl,
            networkIdentifier = acaPyConfig.networkIdentifier,
            baseWalletBpn = acaPyConfig.baseWalletBpn,
            adminApiKey = "", // don't expose the api key outside the AcaPyService
            ledgerType = acaPyConfig.ledgerType,
            ledgerRegistrationUrl = acaPyConfig.ledgerRegistrationUrl
        )
    }

    override suspend fun getWallets(): WalletList {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/multitenancy/wallets")
                headers.append("X-API-Key", acaPyConfig.adminApiKey)
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = subWallet
        }
        return Json.decodeFromString(httpResponse.readText())
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/public?did=$didIdentifier")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/${walletData.walletId}/remove")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            contentType(ContentType.Application.Json)
            body = WalletKey(walletData.walletKey!!)
        }
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/$id/token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            contentType(ContentType.Application.Json)
            body = WalletKey(key)
        }
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/create")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = didCreateDto
        }
    }

    override suspend fun registerDidOnLedger(
        didRegistration: DidRegistration,
        endorserWalletToken: String
    ): DidRegistrationResult {
        return client.post {
            // Role is ignored because endorser cannot register nym with role other than NONE
            url("${acaPyConfig.apiAdminUrl}/ledger/" +
                    "register-nym?" +
                    "did=${didRegistration.did}&" +
                    "verkey=${didRegistration.verkey}&" +
                    "alias=${didRegistration.alias}")
            headers.append(HttpHeaders.Authorization, "Bearer $endorserWalletToken")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun registerNymPublic(registerNymDto: RegisterNymPublicDto) {
        client.post<Any> {
            url(getWalletAndAcaPyConfig().ledgerRegistrationUrl)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = registerNymDto
        }
    }

    override suspend fun registerNymIdunion(registerNymIdunionDto: RegisterNymIdunionDto) {
        client.post<Any> {
            url(getWalletAndAcaPyConfig().ledgerRegistrationUrl)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = registerNymIdunionDto
        }
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/sign")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = signRequest
        }
        return httpResponse.readText()
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/verify")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = verifyRequest
        }
    }

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/resolver/resolve/$did")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                headers.append("X-API-Key", acaPyConfig.adminApiKey)
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            val givenDid = utilsService.replaceSovWithNetworkIdentifier(did)
            throw UnprocessableEntityException("AcaPy Error while resolving DID Doc of $givenDid")
        }
    }

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/set-did-endpoint")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
    }

    override suspend fun deleteConnection(connectionId: String, token: String) {
        client.delete<Any> {
            url("${acaPyConfig.apiAdminUrl}/connections/$connectionId")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun acceptInvitationRequest(connectionId: String, token: String): String {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/connections/$connectionId/accept-request")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
        }
        return httpResponse.readText()
    }

    override suspend fun acceptCredentialOfferBySendingRequest(
        holderDid: String,
        credentialExchangeId: String,
        token: String
    ) {
        val ariesClient = getAcapyClient(token)
        val credRequest = V20CredRequestRequest.builder().holderDid(holderDid).build()
        ariesClient.issueCredentialV2RecordsSendRequest(
            credentialExchangeId,
            credRequest
        )
    }

    override suspend fun acceptCredentialReceivedByStoringIssuedCredential(
        credentialId: String,
        credentialExchangeId: String,
        token: String
    ) {
        val ariesClient = getAcapyClient(token)
        val v20CredStoreRequest = V20CredStoreRequest
            .builder()
            .credentialId(credentialId)
            .build()
        ariesClient.issueCredentialV2RecordsStore(
            credentialExchangeId,
            v20CredStoreRequest
        )
    }

    override fun subscribeForWebSocket(subscriberWallet: WalletExtendedData) {
        val wsUrl = acaPyConfig.apiAdminUrl
            .replace("http", "ws")
            .plus("/ws")

        AriesWebSocketClient
            .builder()
            .bearerToken(subscriberWallet.walletToken)
            .url(wsUrl)
            .apiKey(acaPyConfig.adminApiKey)
            .walletId(subscriberWallet.walletId)
            .handler(
                BaseWalletAriesEventHandler(
                    Services.businessPartnerDataService,
                    Services.walletService,
                    Services.webhookService
                )
            )
            .build()
    }

    override suspend fun getAcapyClient(walletToken: String): AriesClient {
        return AriesClient
            .builder()
            .url(acaPyConfig.apiAdminUrl)
            .apiKey(acaPyConfig.adminApiKey)
            .bearerToken(walletToken)
            .build()
    }

    override suspend fun connect(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto,
        token: String
    ): ConnectionRecord {
        val ariesClient = getAcapyClient(token)
        val didOfPartner = utilsService.replaceNetworkIdentifierWithSov(selfManagedWalletCreateDto.did)
        val pendingCon: Optional<ConnectionRecord> = ariesClient.didExchangeCreateRequest(
            DidExchangeCreateRequestFilter
                .builder()
                .theirPublicDid(didOfPartner)
                .alias(selfManagedWalletCreateDto.name)
                .usePublicDid(true)
                .build()
        )

        return if (pendingCon.isPresent) {
            pendingCon.get()
        } else {
            throw InternalServerErrorException("Connection Request Failed")
        }
    }

    override suspend fun issuanceFlowCredentialSend(
        token: String,
        vc: VerifiableCredentialIssuanceFlowRequest
    ): V20CredExRecord {
        val ariesClient = getAcapyClient(token)

        val idOfCredential = if (!vc.id.isNullOrBlank() && !vc.id.startsWith("http")
            && !vc.id.startsWith("https") && !vc.id.startsWith("urn:uuid") && checkIfUUID(vc.id)) {
                "urn:uuid:${vc.id}"
        } else { vc.id }

        val credential = VerifiableCredential.builder()
            .context(vc.context)
            .credentialSubject(gson.toJsonTree(vc.credentialSubject).asJsonObject)
            .issuanceDate(vc.issuanceDate)
            .issuer(vc.issuerIdentifier)
            .type(vc.type)
            .id(idOfCredential)
            .build()

        val credentialRequest = V2CredentialExchangeFree.builder()
            .connectionId(UUID.fromString(vc.connectionId))
            .filter(
                V2CredentialExchangeFree.V20CredFilter.builder()
                    .ldProof(
                        V2CredentialExchangeFree.LDProofVCDetail.builder()
                            .credential(credential)
                            .options(
                                V2CredentialExchangeFree.LDProofVCDetailOptions.builder()
                                    .proofType(ProofType.Ed25519Signature2018)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            ).build()

        val vc2CredExRecord: Optional<V20CredExRecord> = ariesClient.issueCredentialV2Send(credentialRequest)
        if (vc2CredExRecord.isPresent) {
            if (vc2CredExRecord.get().state == CredentialExchangeState.OFFER_SENT) {
                return vc2CredExRecord.get()
            } else {
                throw InternalServerErrorException("Credential Record is not in OFFER_SENT state. " +
                        "current state: ${vc2CredExRecord.get().state}")
            }
        }
        throw InternalServerErrorException("Failed to issue credential: $credential")
    }

    private fun checkIfUUID(id: String): Boolean {
        return try {
            UUID.fromString(id)
            true
        } catch (exception: IllegalArgumentException) {
            false
        }
    }

}

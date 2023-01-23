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
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.InternalServerErrorException
import org.eclipse.tractusx.managedidentitywallets.models.SelfManagedWalletCreateDto
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialIssuanceFlowRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreateSubWallet
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreateWalletTokenResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.CreatedSubWalletResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidCreate
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidEndpointWithType
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidRegistration
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidRegistrationResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.DidResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.ResolutionResult
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.SignRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyRequest
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.VerifyResponse
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletKey
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletList
import org.hyperledger.acy_py.generated.model.TransactionJobs
import org.hyperledger.acy_py.generated.model.V20CredRequestRequest
import org.hyperledger.acy_py.generated.model.V20CredStoreRequest
import org.hyperledger.aries.AriesClient
import org.hyperledger.aries.AriesWebSocketClient
import org.hyperledger.aries.api.connection.ConnectionFilter
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.connection.ConnectionState
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter
import org.hyperledger.aries.api.endorser.EndorserInfoFilter
import org.hyperledger.aries.api.endorser.SetEndorserInfoFilter
import org.hyperledger.aries.api.endorser.SetEndorserRoleFilter
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree
import org.hyperledger.aries.api.jsonld.ProofType
import org.hyperledger.aries.api.jsonld.VerifiableCredential
import org.hyperledger.aries.api.multitenancy.RemoveWalletRequest
import java.util.*

class AcaPyService(
    private val acaPyConfig: WalletAndAcaPyConfig,
    private val utilsService: UtilsService,
    private val client: HttpClient
) : IAcaPyService {

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).create()

    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        return WalletAndAcaPyConfig(
            networkIdentifier = acaPyConfig.networkIdentifier,
            apiAdminUrl = acaPyConfig.apiAdminUrl,
            baseWalletBpn = acaPyConfig.baseWalletBpn,
            baseWalletDID = acaPyConfig.baseWalletDID,
            baseWalletVerkey = acaPyConfig.baseWalletVerkey,
            adminApiKey = "", // don't expose the api key outside the AcaPyService
            baseWalletAdminUrl = acaPyConfig.baseWalletAdminUrl,
            baseWalletAdminApiKey = "" // don't expose the api key outside the AcaPyService
        )
    }

    override suspend fun getSubWallets(): WalletList {
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

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        val multiTenancyAriesClient = getAcapyClient("")
        multiTenancyAriesClient.multitenancyWalletRemove(
            walletData.walletId!!,
            RemoveWalletRequest.builder().walletKey(walletData.walletKey!!).build()
        )
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

    override suspend fun registerDidOnLedgerUsingBaseWallet(
        didRegistration: DidRegistration
    ): DidRegistrationResult {
        return client.post {
            // Role is ignored because endorser cannot register nym with role other than NONE
            url("${acaPyConfig.baseWalletAdminUrl}/ledger/" +
                    "register-nym?" +
                    "did=${didRegistration.did}&" +
                    "verkey=${didRegistration.verkey}&" +
                    "alias=${didRegistration.alias}")
            headers.append("X-API-Key", acaPyConfig.baseWalletAdminApiKey)
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String?): String {
        val acapyUrlAndApiKey = getAcaPyUrlAndApiKeyBasedOnToken(token)
        val httpResponse: HttpResponse = client.post {
            url("${acapyUrlAndApiKey.first}/jsonld/sign")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acapyUrlAndApiKey.second)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = signRequest
        }
        return httpResponse.readText()
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String?): VerifyResponse {
        val acapyUrlAndApiKey = getAcaPyUrlAndApiKeyBasedOnToken(token)
        return client.post {
            url("${acapyUrlAndApiKey.first}/jsonld/verify")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acapyUrlAndApiKey.second)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = verifyRequest
        }
    }

    override suspend fun resolveDidDoc(did: String, token: String?): ResolutionResult {
        val acapyUrlAndApiKey = getAcaPyUrlAndApiKeyBasedOnToken(token)
        return try {
            client.get {
                url("${acapyUrlAndApiKey.first}/resolver/resolve/$did")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                headers.append("X-API-Key", acapyUrlAndApiKey.second)
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            val givenDid = utilsService.replaceSovWithNetworkIdentifier(did)
            throw UnprocessableEntityException("AcaPy Error while resolving DID Doc of $givenDid")
        }
    }

    override suspend fun updateServiceOfBaseWallet(serviceEndPoint: DidEndpointWithType) {
        client.post<Any> {
            url("${acaPyConfig.baseWalletAdminUrl}/wallet/set-did-endpoint")
            headers.append("X-API-Key", acaPyConfig.baseWalletAdminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
    }

    override suspend fun updateServiceUsingEndorsement(serviceEndPoint: DidEndpointWithType, token: String) {
        val acapyUrlAndApiKey = getAcaPyUrlAndApiKeyBasedOnToken(token)
        client.post<Any> {
            url("${acapyUrlAndApiKey.first}/wallet/set-did-endpoint?create_transaction_for_endorser=true")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acapyUrlAndApiKey.second)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
    }

    override suspend fun deleteConnection(connectionId: String, token: String?) {
        val acapyClient = getAcapyClient(token)
        acapyClient.connectionsRemove(connectionId)
    }

    override suspend fun acceptConnectionRequest(connectionId: String, token: String?): ConnectionRecord {
        val ariesClient = getAcapyClient(token)
        return ariesClient.connectionsAcceptRequest(connectionId, null).orElse(ConnectionRecord())
    }

    override suspend fun acceptCredentialOfferBySendingRequest(
        holderDid: String,
        credentialExchangeId: String,
        token: String?
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
        token: String?
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

    override fun subscribeBaseWalletForWebSocket() {
        val wsUrl = acaPyConfig.baseWalletAdminUrl
            .replace("http", "ws")
            .plus("/ws")
        AriesWebSocketClient
            .builder()
            .url(wsUrl)
            .apiKey(acaPyConfig.baseWalletAdminApiKey)
            .handler(
                BaseWalletAriesEventHandler(
                    Services.businessPartnerDataService,
                    Services.walletService,
                    Services.webhookService
                )
            )
            .build()
    }

    override suspend fun sendConnectionRequest(
        selfManagedWalletCreateDto: SelfManagedWalletCreateDto,
        token: String?
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

    override suspend fun getRequestedConnectionsToBaseWallet(): List<ConnectionRecord> {
        val ariesClient = getAcapyClient(null)
        val connectionFilter: ConnectionFilter = ConnectionFilter.builder()
            .state(ConnectionState.REQUEST)
            .build()
        return ariesClient.connections(connectionFilter).orElseGet { emptyList() }
    }

    override suspend fun setEndorserMetaData(connectionId: String): TransactionJobs? {
        val ariesClient = getAcapyClient(null)
        val txJob = SetEndorserRoleFilter.builder()
            .transactionMyJob(SetEndorserRoleFilter.TransactionJobEnum.TRANSACTION_ENDORSER)
            .build()
        return ariesClient.endorseTransactionSetEndorserRole(connectionId, txJob).orElseGet { null }
    }

    override suspend fun setAuthorRoleAndInfoMetaData(connectionId: String, endorserDID: String, token: String) {
        val ariesClient = getAcapyClient(token)
        val txJob = SetEndorserRoleFilter.builder()
            .transactionMyJob(SetEndorserRoleFilter.TransactionJobEnum.TRANSACTION_AUTHOR)
            .build()
        ariesClient.endorseTransactionSetEndorserRole(connectionId, txJob)
        val endorserInfoFilter = SetEndorserInfoFilter.builder()
            .endorserName("endorser")
            .endorserDid(endorserDID)
            .build()
        ariesClient.endorseTransactionSetEndorserInfo(connectionId, endorserInfoFilter)
    }

    override suspend fun getAcapyClient(walletToken: String?): AriesClient {
        val ariesClient = AriesClient.builder()
        if (walletToken == null) {
            // Catena X Wallet
            ariesClient
                .url(acaPyConfig.baseWalletAdminUrl)
                .apiKey(acaPyConfig.baseWalletAdminApiKey)
        } else {
            // Managed Wallets or multi-tenancy management wallet
            ariesClient
                .url(acaPyConfig.apiAdminUrl)
                .apiKey(acaPyConfig.adminApiKey)
            if (walletToken.isNotBlank()) {
                ariesClient.bearerToken(walletToken)
            }
        }
        return ariesClient.build()
    }

    override suspend fun setDidAsPublicUsingEndorser(did: String, token: String) {
        val ariesClient = getAcapyClient(token)
        val endorserInfoFilter = EndorserInfoFilter.builder()
            .createTransactionForEndorser(true)
            .build()
        // it does not need the connection-id because it uses the connection with the alias `endorser`
        ariesClient.walletDidPublic(did, endorserInfoFilter)
    }


    override suspend fun sendConnectionRequest(
        didOfTheirWallet: String,
        usePublicDid: Boolean,
        alias: String?,
        token: String?,
        label: String?
    ): ConnectionRecord {
        val ariesClient = getAcapyClient(token)
        val pendingCon: Optional<ConnectionRecord> = ariesClient.didExchangeCreateRequest(
            DidExchangeCreateRequestFilter
                .builder()
                .theirPublicDid(utilsService.replaceNetworkIdentifierWithSov(didOfTheirWallet))
                .alias(alias)
                .myLabel(label)
                .usePublicDid(usePublicDid)
                .build()
        )

        return if (pendingCon.isPresent) {
            pendingCon.get()
        } else {
            throw InternalServerErrorException("Connection Request Failed")
        }
    }

    override suspend fun issuanceFlowCredentialSend(
        token: String?,
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

    private fun getAcaPyUrlAndApiKeyBasedOnToken(token: String?): Pair<String, String> {
        if (token == null) {
            // The Catena X Wallet
            return acaPyConfig.baseWalletAdminUrl to acaPyConfig.baseWalletAdminApiKey
        }
        // Other wallets and multi-tenancy management wallet
        return acaPyConfig.apiAdminUrl to acaPyConfig.adminApiKey
    }

}

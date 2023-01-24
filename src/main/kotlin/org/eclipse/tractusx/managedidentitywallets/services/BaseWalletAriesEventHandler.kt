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

package org.eclipse.tractusx.managedidentitywallets.services

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.JsonLdTypes
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.Rfc23State
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.webhook.TenantAwareEventHandler
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

class BaseWalletAriesEventHandler(
        private val businessPartnerDataService: IBusinessPartnerDataService,
        private val walletService: IWalletService,
        private val webhookService: IWebhookService
): TenantAwareEventHandler() {

    private val catenaXCredentialTypes = JsonLdTypes.getCatenaXCredentialTypes()
    private val log = LoggerFactory.getLogger(this::class.java)

    // Connection only with registered wallets
    override fun handleConnection(walletId: String?, connection: ConnectionRecord) {
        super.handleConnection(null, connection)
        log.debug("Connection ${connection.connectionId} is in state ${connection.rfc23State}")
        when(connection.rfc23State) {
            Rfc23State.REQUEST_RECEIVED.toString() -> {
                //TODO accept only from whitelisted public DIDs
                transaction {
                    val theirWallet = walletService.getWallet(
                        identifier = connection.theirLabel, // The BPN
                        withCredentials = false
                    )
                    walletService.addConnection(
                        connectionId = connection.connectionId,
                        connectionOwnerDid = walletService.getBaseWallet().did,
                        connectionTargetDid = theirWallet.did,
                        connectionState = connection.rfc23State
                    )
                    runBlocking {
                        walletService.setEndorserMetaDataForAcapyConnection(connection.connectionId)
                        walletService.acceptConnectionRequest(walletService.getBaseWallet().did, connection)
                    }
                }
            }
            Rfc23State.COMPLETED.toString() -> {
                val storedConnection = walletService.getConnection(connectionId = connection.connectionId)
                if (storedConnection == null) {
                    log.error("Invalid state: Connection ${connection.connectionId} is missing!")
                    return
                }
                val walletOfConnectionTarget: WalletDto = walletService.getWallet(storedConnection.theirDid)

                val webhookUrl: String? = updateConnectionStateAndSendWebhook(connection)

                if (walletOfConnectionTarget.pendingMembershipIssuance) {
                    runBlocking {
                        val successBpnCred = businessPartnerDataService
                            .issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                                targetWallet = walletOfConnectionTarget,
                                connectionId = connection.connectionId,
                                webhookUrl = webhookUrl,
                                type = JsonLdTypes.BPN_TYPE,
                                data = null
                            )
                        val successMembershipCred  = businessPartnerDataService
                            .issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
                                targetWallet = walletOfConnectionTarget,
                                connectionId = connection.connectionId,
                                webhookUrl = webhookUrl,
                                type = JsonLdTypes.MEMBERSHIP_TYPE,
                                data = null
                            )
                        if (successBpnCred.await() && successMembershipCred.await()) {
                            transaction { walletService.setPartnerMembershipIssued(walletOfConnectionTarget) }
                        }
                    }
                }
            }
            Rfc23State.ABANDONED.toString() -> {
                log.error("Connection ${connection.connectionId} is in state ABANDONED")
                updateConnectionStateAndSendWebhook(connection)
            }
            else -> { return }
        }
    }

    private fun updateConnectionStateAndSendWebhook(connection: ConnectionRecord): String? {
        var webhookUrl: String? = null
        transaction {
            val webhook = webhookService.getWebhookByThreadId(connection.requestId)
            if (webhook != null) {
                webhookService.sendWebhookConnectionMessage(webhook.threadId, connection)
                webhookService.updateStateOfWebhook(webhook.threadId, connection.rfc23State)
                webhookUrl = webhook.webhookUrl
            }
            val storedConnection = walletService.getConnection(connection.connectionId)!!
            walletService.updateConnectionState(storedConnection.connectionId, connection.rfc23State)
        }
        return webhookUrl
    }

    override fun handleCredentialV2(walletId: String?, v20Credential: V20CredExRecord?) {
        super.handleCredentialV2(null, v20Credential)
        if (v20Credential != null) {
            log.debug("CredExRecord ${v20Credential.credentialExchangeId} is in state ${v20Credential.state}")
            val threadId = v20Credential.threadId
            when(v20Credential.state) {
                CredentialExchangeState.OFFER_RECEIVED -> {
                    runBlocking {
                        walletService.acceptReceivedOfferVc(walletService.getBaseWallet().did, v20Credential)
                    }
                }
                CredentialExchangeState.CREDENTIAL_ISSUED -> {
                    try {
                        val issuedCred: IssuedVerifiableCredentialRequestDto = Json.decodeFromString(
                            IssuedVerifiableCredentialRequestDto.serializer(),
                            String(Base64.getDecoder().decode(
                                v20Credential.credIssue.credentialsTildeAttach[0].data.base64)
                            )
                        )
                        val holderWallet = walletService.getWallet(issuedCred.credentialSubject["id"] as String)
                        if (holderWallet.isSelfManaged && issuedCred.type.any { catenaXCredentialTypes.contains(it) }) {
                            transaction {
                                walletService.storeCredential(
                                    issuedCred.credentialSubject["id"] as String,
                                    issuedCred
                                )
                            }
                        }
                    } catch (e: Exception) {
                        log.error("Store Credential with thread Id ${v20Credential.threadId} failed!")
                    }
                    transaction {
                        if (webhookService.getWebhookByThreadId(threadId) != null) {
                            webhookService.updateStateOfWebhook(threadId, v20Credential.state.name)
                        }
                    }
                }
                CredentialExchangeState.CREDENTIAL_RECEIVED -> {
                    try {
                        transaction {
                            runBlocking {
                                walletService.acceptAndStoreReceivedIssuedVc(walletService.getBaseWallet().did, v20Credential)
                                if (webhookService.getWebhookByThreadId(threadId) != null) {
                                    webhookService.updateStateOfWebhook(threadId, v20Credential.state.name)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        log.error("Accept And Store Received Issued Verifiable Credentials " +
                                "with threadId ${v20Credential.threadId} failed!")
                    }
                }
                CredentialExchangeState.DONE,
                CredentialExchangeState.ABANDONED,
                CredentialExchangeState.DECLINED -> {
                    transaction {
                        if (webhookService.getWebhookByThreadId(threadId) != null) {
                            webhookService.sendWebhookCredentialMessage(threadId, v20Credential)
                            webhookService.updateStateOfWebhook(threadId, v20Credential.state.name)
                        }
                        if (v20Credential.state !== CredentialExchangeState.DONE) {
                            log.error("CredExRecord ${v20Credential.credentialExchangeId} " +
                                    "is in state ${v20Credential.state}")
                        }
                    }
                }
                else -> { return }
            }
        }
    }
}

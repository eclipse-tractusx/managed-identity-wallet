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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.connection.ConnectionState
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.webhook.TenantAwareEventHandler
import org.jetbrains.exposed.sql.transactions.transaction

class BaseWalletAriesEventHandler(
        private val businessPartnerDataService: IBusinessPartnerDataService,
        private val walletService: IWalletService,
        private val webhookService: IWebhookService
): TenantAwareEventHandler() {

    override fun handleConnection(walletId: String, connection: ConnectionRecord) {
        super.handleConnection(walletId, connection)

        if (connection.state == ConnectionState.COMPLETED) {
            var webhookUrl: String?= null
            val walletOfConnectionTarget: WalletDto = transaction {
                val webhook = webhookService.getWebhookByThreadId(connection.requestId)
                if (webhook != null) {
                    webhookService.sendWebhookConnectionMessage(webhook.threadId, connection)
                    webhookService.updateStateOfWebhook(webhook.threadId, ConnectionState.COMPLETED.name)
                    webhookUrl = webhook.webhookUrl
                }
                val storedConnection = walletService.getConnection(connection.connectionId)
                walletService.updateConnectionState(storedConnection.connectionId, ConnectionState.COMPLETED)
                walletService.getWallet(storedConnection.theirDid, false)
            }

            if (walletOfConnectionTarget.pendingMembershipIssuance) {
                GlobalScope.launch {
                    val success = businessPartnerDataService
                        .issueAndSendCatenaXCredentialsForSelfManagedWalletsAsync(
                            targetWallet = walletOfConnectionTarget,
                            connectionId = connection.connectionId,
                            webhookUrl = webhookUrl
                        ).await()
                    if (success) {
                        transaction {
                            walletService.setPartnerMembershipIssued(walletOfConnectionTarget)
                        }
                    }
                }
            }
        }
    }

    override fun handleCredentialV2(walletId: String?, v20Credential: V20CredExRecord?) {
        super.handleCredentialV2(walletId, v20Credential)
        if (v20Credential != null) {
            val threadId = v20Credential.threadId
            when(v20Credential.state) {
                CredentialExchangeState.CREDENTIAL_ISSUED -> {
                    transaction {
                        val webhook = webhookService.getWebhookByThreadId(threadId)
                        if (webhook != null) {
                            webhookService.sendWebhookCredentialMessage(threadId, v20Credential)
                            webhookService.updateStateOfWebhook(threadId, CredentialExchangeState.CREDENTIAL_ISSUED.name)
                        }
                    }
                }
                CredentialExchangeState.DONE -> {
                    transaction {
                        val webhook = webhookService.getWebhookByThreadId(threadId)
                        if (webhook != null) {
                            webhookService.sendWebhookCredentialMessage(threadId, v20Credential)
                            webhookService.updateStateOfWebhook(threadId, CredentialExchangeState.DONE.name)
                        }
                    }
                }
                CredentialExchangeState.ABANDONED -> {
                    transaction {
                        val webhook = webhookService.getWebhookByThreadId(threadId)
                        if (webhook != null) {
                            webhookService.sendWebhookCredentialMessage(threadId, v20Credential)
                            webhookService.updateStateOfWebhook(threadId, CredentialExchangeState.ABANDONED.name)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

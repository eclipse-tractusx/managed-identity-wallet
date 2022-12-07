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
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.Rfc23State
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.hyperledger.aries.webhook.TenantAwareEventHandler
import org.slf4j.LoggerFactory

class ManagedWalletsAriesEventHandler(
    private val walletService: IWalletService,
    private val utilsService: UtilsService
) : TenantAwareEventHandler() {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    override fun handleConnection(walletId: String, connection: ConnectionRecord) {
        super.handleConnection(walletId, connection)
        val wallet = walletService.getWallet(walletId)
        if (walletService.getCatenaXWalletWithoutSecrets().walletId == walletId) {
            return // Catena X wallet
        }
        when (connection.rfc23State) {
            Rfc23State.REQUEST_RECEIVED.toString() -> {
                runBlocking {
                    log.debug(
                        "ConnectionRecord ${connection.connectionId} for wallet ${wallet.bpn} " +
                                "is in state ${Rfc23State.REQUEST_RECEIVED}"
                    )
                    walletService.acceptConnectionRequest(walletId, connection)
                }
            }
            Rfc23State.COMPLETED.toString() -> {
                runBlocking {
                    log.debug(
                        "ConnectionRecord ${connection.connectionId} for wallet ${wallet.bpn} " +
                                "is in state ${Rfc23State.COMPLETED}"
                    )
                    val externalWalletDid = utilsService.getDidMethodPrefixWithNetworkIdentifier() +
                            (connection.theirPublicDid ?: connection.theirDid)
                    walletService.addConnection(
                        connectionId = connection.connectionId,
                        managedWalletDid = wallet.did, // connection.myDid is one of the internal DIDs of the Wallet
                        externalWalletDid = externalWalletDid,
                        connectionState = connection.rfc23State
                    )
                }
            }
            Rfc23State.ABANDONED.toString() -> {
                log.error(
                    "Connection with id ${connection.connectionId} for wallet ${wallet.bpn} " +
                            "is in state abandoned"
                )
            }
            else -> {
                log.debug(
                    "Connection with id ${connection.connectionId} for wallet ${wallet.bpn} " +
                            "is in state ${connection.rfc23State}"
                )
            }
        }
    }

    override fun handleCredentialV2(walletId: String?, v20Credential: V20CredExRecord?) {
        super.handleCredentialV2(walletId, v20Credential)
        if (walletService.getCatenaXWalletWithoutSecrets().walletId == walletId) {
            return // Catena X wallet
        }
        if (v20Credential != null) {
            when (v20Credential.state) {
                CredentialExchangeState.OFFER_RECEIVED -> {
                    runBlocking {
                        log.debug("CredExRecord ${v20Credential.credentialExchangeId} is in state OFFER_RECEIVED")
                        walletService.acceptReceivedOfferVc(walletId!!, v20Credential)
                    }
                }
                CredentialExchangeState.CREDENTIAL_RECEIVED -> {
                    runBlocking {
                        log.debug("CredExRecord ${v20Credential.credentialExchangeId} is in state CREDENTIAL_RECEIVED")
                        // store credential
                        walletService.acceptReceivedIssuedVc(walletId!!, v20Credential)
                    }
                }
                CredentialExchangeState.DONE -> {
                    log.debug("CredExRecord ${v20Credential.credentialExchangeId} is in state DONE")
                }
                CredentialExchangeState.ABANDONED -> {
                    log.error("CredExRecord ${v20Credential.credentialExchangeId} is in state ABANDONED")
                }
                else -> {
                    log.debug("CredExRecord ${v20Credential.credentialExchangeId} is in state ${v20Credential.state}")
                }
            }
        }
    }
}

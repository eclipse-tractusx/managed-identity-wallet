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

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import kotlinx.coroutines.Deferred
import org.eclipse.tractusx.managedidentitywallets.models.BPDMConfig
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.slf4j.LoggerFactory

/**
 * The IBusinessPartnerDataService interface describe the functionalities required
 * for pulling data from the BPDM and issuing/updating credentials.
 */
interface IBusinessPartnerDataService {

    /**
     * Asynchronously pulls data from BPDM, and updates the credentials stored for each legal entity.
     * @param identifier the identifier of a specific legal entity for which the credentials need to be updated
     * @return A deferred boolean. It returns (when waited) true if the update was successful, else false
     */
    suspend fun pullDataAndUpdateBaseWalletCredentialsAsync(identifier: String? = null): Deferred<Boolean>

    /**
     * Asynchronously issues credentials by base wallet and store them.
     * @param walletHolderDto the wallet of the holder
     * @param type The type of the credential
     * @param data The data that is required to generate the credential, it can be null if not required
     * @return A deferred boolean. It returns (when waited) true if the issuance was successful, else false
     */
    suspend fun issueAndStoreBaseWalletCredentialsAsync(
        walletHolderDto: WalletDto,
        type: String,
        data: Any? = null
    ): Deferred<Boolean>


    /**
     * Asynchronously issues credentials by base wallet for self-managed wallets.
     * @param targetWallet the data of the target wallet
     * @param connectionId the id of the connection between the base wallet and the target wallet
     * @param webhookUrl the url of the webhook to be notified when the credential is issued
     * @param type type of the credential to be issued
     * @param data generic data that will be included in the credential to be issued
     * @return a Deferred boolean. It returns (when waited) true if the operation was successful, else false
     */
    suspend fun issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
        targetWallet: WalletDto,
        connectionId: String,
        webhookUrl: String? = null,
        type: String,
        data: Any? = null
    ): Deferred<Boolean>

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        /**
         * Creates the business partner data service which implements the IBusinessPartnerDataService.
         * The used HTTP client to communicate with the BPDM is configured in this method.
         */
        fun createBusinessPartnerDataService(
            walletService: IWalletService,
            bpdmConfig: BPDMConfig
        ): IBusinessPartnerDataService {
            return BusinessPartnerDataServiceImpl(
                walletService,
                bpdmConfig,
                HttpClient {
                    expectSuccess = false // must be false to handle thrown error if the access token has expired
                    install(ResponseObserver) {
                        onResponse { response ->
                            log.debug("HTTP status: ${response.status.value}")
                            log.debug("HTTP description: ${response.status.description}")
                        }
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 30000
                        connectTimeoutMillis = 30000
                        socketTimeoutMillis = 30000
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.BODY
                    }
                })
        }
    }
}

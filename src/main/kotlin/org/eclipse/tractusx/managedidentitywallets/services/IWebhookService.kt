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

import io.ktor.client.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Webhook
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.slf4j.LoggerFactory

/**
 * The IWebhookService interface for management and notifications of webhooks.
 */
interface IWebhookService {

    /**
     * Adds a new webhook with given threadId, url and state
     * @param threadId the threadId of the webhook
     * @param url the url of the webhook
     * @param state the state of the webhook
     */
    fun addWebhook(threadId: String, url:String, state: String)

    /**
     * Gets the webhook by threadId
     * @param threadId the threadId of the webhook
     * @return the [Webhook] if found, otherwise null
     */
    fun getWebhookByThreadId(threadId: String?): Webhook?

    /**
     * Sends the webhook message to given url
     * @param url the url of the webhook
     * @param connection the connection record as message
     * @return true if the webhook message was sent successfully, otherwise false
     */
    fun sendWebhookConnectionMessage(url: String, connection: ConnectionRecord): Boolean

    /**
     * Sends the webhook message to given url
     * @param url the url of the webhook
     * @param v20CredExRecord the credential as message
     * @return true if the webhook message was sent successfully, otherwise false
    */
    fun sendWebhookCredentialMessage(url: String, v20CredExRecord: V20CredExRecord): Boolean

    /**
     * Updates the state of webhook with given threadId and state
     * @param threadId the threadId of the webhook
     * @param state the state of the webhook
     * @throws NotFoundException if the Webhook does not exist
     */
    fun updateStateOfWebhook(threadId: String, state: String)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        /**
         * Creates the Webhook Service which implements the IWebhookService.
         * The used HTTP client to send webhook messages is configured in this method.
         */
        fun createWebhookService(
            webhookRepository: WebhookRepository
        ): IWebhookService {
            return WebhookServiceImpl(
                webhookRepository,
                HttpClient {
                    expectSuccess = false // must be set to false to handle thrown error if the access token has expired
                    install(ResponseObserver) {
                        onResponse { response ->
                            log.debug("HTTP status: ${response.status.value}")
                            log.debug("HTTP description: ${response.status.description}")
                        }
                    }
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.BODY
                    }
                }
            )
        }
    }
}

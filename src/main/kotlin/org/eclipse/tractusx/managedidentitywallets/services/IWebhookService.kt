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
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Webhook
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.slf4j.LoggerFactory

interface IWebhookService {

    fun addWebhook(threadId: String, url:String, state: String)

    fun getWebhookByThreadId(threadId: String?): Webhook?

    fun sendWebhookConnectionMessage(url: String, connection: ConnectionRecord): Boolean

    fun sendWebhookCredentialMessage(url: String, v20CredExRecord: V20CredExRecord): Boolean

    fun sendWebhookPresentationMessage(url: String): Boolean

    fun updateStateOfWebhook(threadId: String, state: String)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

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

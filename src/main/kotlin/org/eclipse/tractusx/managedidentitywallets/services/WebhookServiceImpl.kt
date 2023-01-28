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
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Webhook
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.hyperledger.aries.api.connection.ConnectionRecord
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord
import org.jetbrains.exposed.sql.transactions.transaction

class WebhookServiceImpl(
    private val webhookRepository: WebhookRepository,
    private val client: HttpClient
) : IWebhookService {

    override fun updateStateOfWebhook(threadId: String, state: String) {
        webhookRepository.updateStateOfWebhook(threadId, state)
    }

    override fun addWebhook(threadId: String, url: String, state: String) {
        webhookRepository.add(
            webhookThreadId = threadId,
            url = url,
            stateOfRequest = state
        )
    }

    override fun getWebhookByThreadId(threadId: String?): Webhook? {
        if (!threadId.isNullOrBlank()) {
            return transaction { webhookRepository.getOrNull(threadId) }
        }
        return null
    }

    override fun sendWebhookConnectionMessage(url: String, connection: ConnectionRecord): Boolean {
        return runBlocking {
             sendWebhookMessage(url, connection)
        }
    }

    override fun sendWebhookCredentialMessage(url: String, v20CredExRecord: V20CredExRecord): Boolean {
        return runBlocking {
            sendWebhookMessage(url, v20CredExRecord)
        }
    }

    override fun sendWebhookPresentationMessage(url: String): Boolean {
        return runBlocking {
            sendWebhookMessage(url, "empty")
        }
    }

    private suspend fun sendWebhookMessage(url: String, messageBody: Any): Boolean {
        return try {
            client.post<Any> {
                url(url)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = messageBody
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

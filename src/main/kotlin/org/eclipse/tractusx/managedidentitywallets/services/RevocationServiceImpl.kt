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
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.CredentialStatus
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto

class RevocationServiceImpl(
        private val revocationUrl: String,
        private val client: HttpClient
    ): IRevocationService {

    override suspend fun registerList(profileName: String, issueCredential: Boolean): String {
        val httpResponse: HttpResponse = client.post {
            url("$revocationUrl/management/lists?profile=$profileName&issue-list-credential=$issueCredential")
            accept(ContentType.Any)
        }
        return httpResponse.readText()
    }

    override suspend fun addStatusEntry(profileName: String): CredentialStatus {
        var httpResponse: HttpResponse = client.config {
            expectSuccess = false
        }.post {
            url("$revocationUrl/management/lists/$profileName/entry")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        if (httpResponse.status == HttpStatusCode.NotFound) {
            // create and retry to implement self healing if list for the bpn was not created yet
            registerList(profileName, false)
            httpResponse = client.post {
                url("$revocationUrl/management/lists/$profileName/entry")
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        } else if (httpResponse.status.value >= 300) {
            throw UnprocessableEntityException(httpResponse.readText())
        }
        return Json.decodeFromString(httpResponse.readText())
    }

    override suspend fun getStatusListCredentialOfManagedWallet(listName: String): VerifiableCredentialDto {
        return getStatusListCredentialOfUrl("$revocationUrl/status/$listName")
    }

    override suspend fun getStatusListCredentialOfUrl(statusListUrl: String): VerifiableCredentialDto {
        return try {
            val httpResponse: HttpResponse = client.get {
                url(statusListUrl)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
            Json.decodeFromString(httpResponse.readText())
        } catch (e: Exception) {
            if (!e.message.isNullOrBlank() && e.message!!.contains("404 Not Found")) {
                throw NotFoundException("The StatusList credential is not found!")
            }
            throw UnprocessableEntityException("The StatusList credential is not available due to the Error: ${e.message}")
        }
    }

    override suspend fun revoke(profileName: String, indexOfCredential: Long) {
        client.delete<HttpResponse> {
            url("$revocationUrl/management/lists/$profileName/entry/$indexOfCredential")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun issueStatusListCredentials(profileName: String?, force: Boolean?) {
        if (profileName.isNullOrBlank()) {
            client.post<HttpResponse> {
                url("$revocationUrl/management/lists/issue-credentials")
                accept(ContentType.Application.Json)
            }
        } else {
            client.post<HttpResponse> {
                url("$revocationUrl/management/lists/issue-credential/$profileName?force=$force")
                accept(ContentType.Application.Json)
            }
        }

    }

}

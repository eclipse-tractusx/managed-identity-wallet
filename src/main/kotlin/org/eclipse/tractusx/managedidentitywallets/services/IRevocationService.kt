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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.observer.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.CredentialStatus
import io.ktor.client.features.logging.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.slf4j.LoggerFactory

interface IRevocationService {

    suspend fun registerList(profileName: String, issueCredential: Boolean): String

    suspend fun addStatusEntry(profileName: String): CredentialStatus

    suspend fun getStatusListCredentialOfManagedWallet(listName: String): VerifiableCredentialDto

    suspend fun getStatusListCredentialOfUrl(statusListUrl: String): VerifiableCredentialDto

    suspend fun revoke(profileName: String, indexOfCredential: Long)

    suspend fun issueStatusListCredentials(profileName: String? = null, force: Boolean? = false)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        fun createRevocationService(revocationUrl: String): IRevocationService {
            return RevocationServiceImpl(
                revocationUrl,
                HttpClient {
                    expectSuccess = true
                    install(HttpTimeout) {
                        requestTimeoutMillis = 30000
                        connectTimeoutMillis = 30000
                        socketTimeoutMillis = 30000
                    }
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
                    install(JsonFeature) {
                        serializer = JacksonSerializer {
                            enable(SerializationFeature.INDENT_OUTPUT)
                            serializationConfig.defaultPrettyPrinter
                            setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        }
                    }
                })
        }
    }
}

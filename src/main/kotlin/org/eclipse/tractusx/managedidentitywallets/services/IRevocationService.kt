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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.features.observer.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.CredentialStatus
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.slf4j.LoggerFactory

/**
 * The IRevocationService interface describes the functionalities
 * for managing and issuing revocation lists and status credentials.
 */
interface IRevocationService {

    /**
     * Registers a new revocation list with the given profile name.
     * @param profileName the name of the profile for the revocation list
     * @param issueCredential a flag indicating whether to issue a Verifiable Credential directly for the list
     * @return the response a string
     */
    suspend fun registerList(profileName: String, issueCredential: Boolean): String

    /**
     * Adds a new status entry to an existing revocation list.
     * @param profileName the name of the profile for the revocation list
     * @return [CredentialStatus] the status of the added entry
     * @throws NotFoundException if the list does not exist
     * @throws UnprocessableEntityException if adding the status entry failed
     */
    suspend fun addStatusEntry(profileName: String): CredentialStatus

    /**
     * Retrieves the status list credential for a given managed wallet using its listName
     * @param listName the name of the revocation list
     * @return [VerifiableCredentialDto] the status list Verifiable Credential
     * @throws NotFoundException if the credential is not found
     * @throws UnprocessableEntityException if the credential could not be retrieved
     */
    suspend fun getStatusListCredentialOfManagedWallet(listName: String): VerifiableCredentialDto

    /**
     * Retrieves the status list credential from a given URL
     * @param statusListUrl the URL for the status list credential
     * @return [VerifiableCredentialDto] the status list Verifiable Credential
     * @throws NotFoundException if the credential is not found
     * @throws UnprocessableEntityException if the credential could not be retrieved
     */
    suspend fun getStatusListCredentialOfUrl(statusListUrl: String): VerifiableCredentialDto

    /**
     * Revokes an issued credential from an existing revocation list
     * @param profileName the name of the profile for the revocation list
     * @param indexOfCredential the index of the credential
     */
    suspend fun revoke(profileName: String, indexOfCredential: Long)

    /**
     * Issues status list credentials for a given profile or all profiles
     * @param profileName the profile to issue credentials for. If null, credentials will be issued for all profiles
     * @param force a flag indicating whether to force the issuance of credentials ignoring the cache in Revocation Service
     */
    suspend fun issueStatusListCredentials(profileName: String? = null, force: Boolean? = false)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        /**
         * Creates the revocation Service which implements the IRevocationService.
         * The used HTTP client to communicate with AcaPy is configured in this method
         */
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

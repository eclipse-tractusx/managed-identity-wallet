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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.*

class AcaPyService(
    private val acaPyConfig: WalletAndAcaPyConfig,
    private val utilsService: UtilsService,
    private val client: HttpClient): IAcaPyService {

    override fun getWalletAndAcaPyConfig(): WalletAndAcaPyConfig {
        return WalletAndAcaPyConfig(
            apiAdminUrl = acaPyConfig.apiAdminUrl,
            networkIdentifier = acaPyConfig.networkIdentifier,
            baseWalletBpn = acaPyConfig.baseWalletBpn,
            adminApiKey = "" // don't expose the api key outside the AcaPyService
        )
    }

    override suspend fun getWallets(): WalletList {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/multitenancy/wallets")
                headers.append("X-API-Key", acaPyConfig.adminApiKey)
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    override suspend fun createSubWallet(subWallet: CreateSubWallet): CreatedSubWalletResult {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = subWallet
        }
        return Json.decodeFromString(httpResponse.readText())
    }

    override suspend fun assignDidToPublic(didIdentifier: String, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/public?did=$didIdentifier")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun deleteSubWallet(walletData: WalletExtendedData) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/${walletData.walletId}/remove")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            contentType(ContentType.Application.Json)
            body = WalletKey(walletData.walletKey)
        }
    }

    override suspend fun getTokenByWalletIdAndKey(id: String, key: String): CreateWalletTokenResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/multitenancy/wallet/$id/token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            contentType(ContentType.Application.Json)
            body = WalletKey(key)
        }
    }

    override suspend fun createLocalDidForWallet(didCreateDto: DidCreate, token: String): DidResult {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/wallet/did/create")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = didCreateDto
        }
    }

    override suspend fun registerDidOnLedger(
        didRegistration: DidRegistration,
        endorserWalletToken: String
    ): DidRegistrationResult {
        return client.post {
            // Role is ignored because endorser cannot register nym with role other than NONE
            url("${acaPyConfig.apiAdminUrl}/ledger/" +
                    "register-nym?" +
                    "did=${didRegistration.did}&" +
                    "verkey=${didRegistration.verkey}&" +
                    "alias=${didRegistration.alias}")
            headers.append(HttpHeaders.Authorization, "Bearer $endorserWalletToken")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
        }
    }

    override suspend fun <T> signJsonLd(signRequest: SignRequest<T>, token: String): String {
        val httpResponse: HttpResponse = client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/sign")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = signRequest
        }
        return httpResponse.readText()
    }

    override suspend fun <T> verifyJsonLd(verifyRequest: VerifyRequest<T>, token: String): VerifyResponse {
        return client.post {
            url("${acaPyConfig.apiAdminUrl}/jsonld/verify")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = verifyRequest
        }
    }

    override suspend fun resolveDidDoc(did: String, token: String): ResolutionResult {
        return try {
            client.get {
                url("${acaPyConfig.apiAdminUrl}/resolver/resolve/$did")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                headers.append("X-API-Key", acaPyConfig.adminApiKey)
                accept(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            val givenDid = utilsService.replaceSovWithNetworkIdentifier(did)
            throw UnprocessableEntityException("AcaPy Error while resolving DID Doc of $givenDid")
        }
    }

    override suspend fun updateService(serviceEndPoint: DidEndpointWithType, token: String) {
        client.post<Any> {
            url("${acaPyConfig.apiAdminUrl}/wallet/set-did-endpoint")
            headers.append(HttpHeaders.Authorization, "Bearer $token")
            headers.append("X-API-Key", acaPyConfig.adminApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = serviceEndPoint
        }
    }

}

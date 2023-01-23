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

@file:UseSerializers(AnySerializer::class)

package org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.eclipse.tractusx.managedidentitywallets.models.ssi.DidDocumentDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiablePresentationDto
import org.eclipse.tractusx.managedidentitywallets.plugins.AnySerializer

enum class EndPointType { Endpoint, Profile, LinkedDomains }

enum class KeyType { ED25519 { override fun toString() = "ed25519" } }

enum class DidMethod { SOV { override fun toString() = "sov" }}

enum class WalletType {
    INDY { override fun toString() = "indy" },
    ASKAR { override fun toString() = "askar"}
}

enum class KeyManagementMode {
    MANAGED { override fun toString() = "managed" },
    UNMANAGED { override fun toString() = "unmanaged" }
}

enum class WalletDispatchType {
    BASE { override fun toString() = "base" },
    DEFAULT { override fun toString() = "default" }
}

enum class VerificationKeyType {
    PUBLIC_KEY_BASE58 { override fun toString() = "publicKeyBase58" }
}

enum class Rfc23State {
    START { override fun toString() = "start" },
    INVITATION_RECEIVED { override fun toString() = "invitation-received" },
    REQUEST_SENT { override fun toString() = "request-sent" },
    REQUEST_RECEIVED { override fun toString() = "request-received" },
    RESPONSE_SENT { override fun toString() = "response-sent" },
    RESPONSE_RECEIVED { override fun toString() = "response-received" },
    COMPLETED { override fun toString() = "completed" },
    ABANDONED { override fun toString() = "abandoned" }
}

@Serializable
data class WalletAndAcaPyConfig(
    val networkIdentifier: String,
    val baseWalletBpn: String,
    val baseWalletDID: String,
    val baseWalletVerkey: String,
    val apiAdminUrl: String,
    val adminApiKey: String,
    val baseWalletAdminUrl: String,
    val baseWalletAdminApiKey: String
)

@Serializable
data class WalletList(val results: List<WalletRecord>)

@Serializable
data class WalletRecord(
    @SerialName("created_at") @JsonProperty("created_at") val createdAt: String,
    @SerialName("wallet_id") @JsonProperty("wallet_id") val walletId: String,
    @SerialName("key_management_mode") @JsonProperty("key_management_mode") val keyManagementMode: String,
    @SerialName("updated_at") @JsonProperty("updated_at") val updatedAt: String,
    val settings: WalletSettings,
    val state: String? = null
)

@Serializable
data class WalletSettings(
    @SerialName("wallet.type") @JsonProperty("wallet.type") val walletType: String,
    @SerialName("wallet.name") @JsonProperty("wallet.name") val walletName: String,
    @SerialName("wallet.webhook_urls") @JsonProperty("wallet.webhook_urls") val walletWebhookUrls: List<String>,
    @SerialName("wallet.dispatch_type") @JsonProperty("wallet.dispatch_type") val walletDispatchType: String,
    @SerialName("wallet.id") @JsonProperty("wallet.id") val walletId: String,
    @SerialName("default_label") @JsonProperty("default_label") val defaultLabel: String,
    @SerialName("image_url") @JsonProperty("image_url") val imageUrl: String? = ""
)

@Serializable
data class CreateSubWallet(
    @SerialName("image_url") @JsonProperty("image_url") val imageUrl: String? = "",
    @SerialName("key_management_mode") @JsonProperty("key_management_mode") val keyManagementMode: String,
    val label: String,
    @SerialName("wallet_webhook_urls") @JsonProperty("wallet_webhook_urls") val walletWebhookUrls: List<String>,
    @SerialName("wallet_dispatch_type") @JsonProperty("wallet_dispatch_type") val walletDispatchType: String,
    @SerialName("wallet_key") @JsonProperty("wallet_key") val walletKey : String,
    @SerialName("wallet_name") @JsonProperty("wallet_name") val walletName: String,
    @SerialName("wallet_type") @JsonProperty("wallet_type") val walletType: String
)

@Serializable
data class CreatedSubWalletResult(
    @SerialName("created_at") @JsonProperty("created_at") val createdAt: String,
    @SerialName("wallet_id") @JsonProperty("wallet_id") val walletId: String,
    @SerialName("key_management_mode") @JsonProperty("key_management_mode") val keyManagementMode: String,
    @SerialName("updated_at") @JsonProperty("updated_at") val updatedAt: String,
    val settings: WalletSettings,
    val token: String
)

@Serializable
data class DidCreate(val method: String, val options: DidCreateOptions)

@Serializable
data class DidCreateOptions(@SerialName("key_type") @JsonProperty("key_type") val keyType: String)

@Serializable
data class DidResult(val result: DidResultDetails)

@Serializable
data class DidResultDetails(
    val did: String,
    @SerialName("key_type") @JsonProperty("key_type") val keyType: String,
    val method: String,
    val posture: String,
    val verkey: String
)

@Serializable
data class WalletKey(@SerialName("wallet_key") @JsonProperty("wallet_key") val walletKey: String)

@Serializable
data class CreateWalletTokenResponse(val token: String)

@Serializable
data class DidRegistration(
    val alias: String? = null,
    val did: String,
    val role: String? = "ENDORSER",
    val verkey: String
)

@Serializable
data class DidRegistrationResult(
    val success: Boolean
)

@Serializable
data class SignRequest<T>(
    val doc: SignDoc<T>,
    val verkey: String
)

@Serializable
data class SignDoc<T>(
    val credential: T,
    val options: SignOptions
)

@Serializable
data class SignOptions(
    val challenge: String? = null,
    val domain: String? = null,
    val proofPurpose: String,
    val type: String,
    val verificationMethod: String
)

@Serializable
data class SignCredentialResponse(
    val error: String? = null,
    @JsonProperty("signed_doc") @SerialName("signed_doc") val signedDoc: VerifiableCredentialDto? = null
)

@Serializable
data class SignPresentationResponse(
    val error: String? = null,
    @JsonProperty("signed_doc") @SerialName("signed_doc") val signedDoc: VerifiablePresentationDto? = null
)

@Serializable
data class VerifyResponse(
    val error: String? = null,
    val valid: Boolean,
    val vp: VerifiablePresentationDto? = null
)

@Serializable
data class VerifyRequest<T>(
    @JsonProperty("doc") @SerialName("doc") val signedDoc: T?,
    val verkey: String? = null
)

@Serializable
data class ResolutionResult(
    @JsonProperty("did_document") @SerialName("did_document")  val didDoc: DidDocumentDto,
    val metadata: ResolutionMetaData
)

@Serializable
data class ResolutionMetaData(
    @JsonProperty("resolver_type") @SerialName("resolver_type") val resolverType: String,
    val resolver: String,
    @JsonProperty("retrieved_time") @SerialName("retrieved_time") val retrievedTime: String,
    val duration: Int
)

@Serializable
data class DidEndpointWithType(
    @JsonProperty("did") @SerialName("did") val didIdentifier: String,
    val endpoint: String,
    @JsonProperty("endpoint_type") @SerialName("endpoint_type") val endpointType: String
)

@Serializable
data class CredentialOfferResponse(
    @JsonProperty("credential_offer") @SerialName("credential_offer") val credentialOffer: String,
    @JsonProperty("threadId") @SerialName("threadId") val threadId: String
)

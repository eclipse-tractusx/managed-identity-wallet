@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models.ssi.acapy

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.catenax.core.custodian.models.ssi.DidDocumentDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto
import net.catenax.core.custodian.models.ssi.VerifiablePresentationDto
import net.catenax.core.custodian.plugins.AnySerializer

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
){}

@Serializable
data class WalletSettings(
    @SerialName("wallet.type") @JsonProperty("wallet.type") val walletType: String,
    @SerialName("wallet.name") @JsonProperty("wallet.name") val walletName: String,
    @SerialName("wallet.webhook_urls") @JsonProperty("wallet.webhook_urls") val walletWebhookUrls: List<String>,
    @SerialName("wallet.dispatch_type") @JsonProperty("wallet.dispatch_type") val walletDispatchType: String,
    @SerialName("wallet.id") @JsonProperty("wallet.id") val walletId: String,
    @SerialName("default_label") @JsonProperty("default_label") val defaultLabel: String,
    @SerialName("image_url") @JsonProperty("image_url") val imageUrl: String? = ""
) {}

@Serializable
data class CreateSubWallet(
    @SerialName("image_url") @JsonProperty("image_url") val imageUrl: String? = "",
    @SerialName("key_management_mode") @JsonProperty("key_management_mode") val keyManagementMode: String,
    val label: String,
    @SerialName("wallet.webhook_urls") @JsonProperty("wallet.webhook_urls") val walletWebhookUrls: List<String>,
    @SerialName("wallet.dispatch_type") @JsonProperty("wallet.dispatch_type") val walletDispatchType: String,
    @SerialName("wallet_key") @JsonProperty("wallet_key") val walletKey : String,
    @SerialName("wallet_name") @JsonProperty("wallet_name") val walletName: String,
    @SerialName("wallet_type") @JsonProperty("wallet_type") val walletType: String
) {}

@Serializable
data class CreatedSubWalletResult(
    @SerialName("created_at") @JsonProperty("created_at") val createdAt: String,
    @SerialName("wallet_id") @JsonProperty("wallet_id") val walletId: String,
    @SerialName("key_management_mode") @JsonProperty("key_management_mode") val keyManagementMode: String,
    @SerialName("updated_at") @JsonProperty("updated_at") val updatedAt: String,
    val settings: WalletSettings,
    val token: String
) {}

@Serializable
data class DidCreate(
    val method: String,
    val options: DidCreateOptions
) {}

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
    val seed: String? = null,
    val did: String,
    val verkey: String
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
    val valid: Boolean
)

@Serializable
data class VerifyRequest<T>(
    @JsonProperty("doc") @SerialName("doc")val signedDoc: T?,
    val verkey: String
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

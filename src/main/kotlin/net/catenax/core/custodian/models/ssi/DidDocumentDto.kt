@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models.ssi

import com.fasterxml.jackson.annotation.JsonProperty
import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.catenax.core.custodian.plugins.AnySerializer

@Serializable
data class DidDocumentDto(
    val id: String,
    @SerialName("@context") @JsonProperty("@context")
    val context: List<String>,
    val alsoKnownAs: String? = null,
    @Serializable(AnySerializer::class) val controller: Any? = null,
    @SerialName("verificationMethod")
    @JsonProperty("verificationMethod")
    val verificationMethods: List<DidVerificationMethodDto>? = null,
    @SerialName("authentication")
    @JsonProperty("authentication")
    val authenticationVerificationMethods: List<Any>? = null,
    @SerialName("assertionMethod")
    @JsonProperty("assertionMethod")
    val assertionMethodVerificationMethods: List<Any>? = null,
    @SerialName("keyAgreement")
    @JsonProperty("keyAgreement")
    val keyAgreementVerificationMethods: List<Any>? = null,
    @SerialName("capabilityInvocation")
    @JsonProperty("capabilityInvocation")
    val capabilityInvocationVerificationMethods: List<Any>? = null,
    @SerialName("capabilityDelegation")
    @JsonProperty("capabilityDelegation")
    val capabilityDelegationVerificationMethods: List<Any>? = null,
    @SerialName("service")
    @JsonProperty("service")
    val services: List<DidServiceDto>? = null
)

@Serializable
data class DidDocumentParameters(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of Entity", name = "identifier")
    val identifier: String
)

package net.catenax.core.custodian.models.ssi

import com.fasterxml.jackson.annotation.JsonProperty
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.catenax.core.custodian.plugins.AnySerializer

@Serializable
data class DidDocumentDto(
    val id: String,
    @JsonProperty("@context")
    @SerialName("@context")
    val context: List<String>,
    val alsoKnownAs: String? = null,
    @Serializable(AnySerializer::class) val controller: Any? = null,
    val verificationMethods: List<DidVerificationMethodDto>? = null,
    val authenticationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val assertionMethodVerificationMethods: List<DidVerificationMethodDto>? = null,
    val keyAgreementVerificationMethods: List<DidVerificationMethodDto>? = null,
    val capabilityInvocationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val capabilityDelegationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val services: List<DidServiceDto>? = null
) {}

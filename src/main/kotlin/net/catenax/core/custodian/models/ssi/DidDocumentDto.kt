package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonNames
import net.catenax.core.custodian.plugins.AnySerializer

@Serializable
class DidDocumentDto(
    val id: String,
    @JsonNames("@context") val context: List<String>,
    val type: List<String>,
    val verificationMethods: List<DidVerificationMethodDto>? = null,
    val authenticationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val assertionMethodVerificationMethods: List<DidVerificationMethodDto>? = null,
    val keyAgreementVerificationMethods: List<DidVerificationMethodDto>? = null,
    val capabilityInvocationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val capabilityDelegationVerificationMethods: List<DidVerificationMethodDto>? = null,
    val services: List<DidServiceDto>? = null
) {}

package net.catenax.core.custodian.models.ssi

import com.fasterxml.jackson.annotation.JsonProperty
import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


@Serializable
data class VerifiablePresentationRequestDto(
    @JsonProperty("@context")
    @SerialName("@context")
    @Field(description = "List of contexts", name = "@context")
    val context: List<String>,
    @Field(description = "List of types", name = "type")
    val type: List<String>,
    @Field(description = "The DID of the holder as String (URI compatible)", name = "holder")
    val holder: String,
    @Field(description = "List of Verifiable Credential IDs for stored credentials with IDs", name = "verifiableCredentialIds")
    val verifiableCredentialIds: List<String>? = null,
    @Field(description = "List of Verifiable Credentials", name = "verifiableCredentials")
    val verifiableCredentials: List<VerifiableCredentialDto>? = null
) { }

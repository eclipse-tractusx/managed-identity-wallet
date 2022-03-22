package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.Serializable

@Serializable
data class DidServiceDto(
    @Field(description = "The ID of the Service Endpoint as String (URI compatible)", name = "id")
    val id: String,
    @Field(description = "The Type of the Service Endpoint as String", name = "type")
    val type: String,
    @Field(description = "The URL of the Service Endpoint as String (URI compatible)", name = "serviceEndpoint")
    val serviceEndpoint: String,
    val recipientKeys: List<String>? = null,
    val routingKeys: List<String>? = null,
    val priority: Int? = null
)

@Serializable
data class DidServiceUpdateRequestDto(
    @Field(description = "The Type of the Service Endpoint as String", name = "type")
    val type: String,
    @Field(description = "The URL of the Service Endpoint as String (URI compatible)", name = "serviceEndpoint")
    val serviceEndpoint: String
)

@Serializable
data class DidDocumentServiceParameters(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID or BPN of Entity", name = "identifier")
    val identifier: String,
    @Param(type = ParamType.PATH)
    @Field(description = "The ID of the Service Endpoint as String (URI compatible)", name = "id")
    val id: String
)

package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.Serializable

@Serializable
data class DidServiceDto(
    @Field(description = "The ID of the Service Endpoint as String (URI compatible)", name = "id")
    val id: String,
    @Field(description = "The Type of the Service Endpoint as String", name = "type")
    val type: String,
    @Field(description = "The Type of the Service Endpoint as String (URI compatible)", name = "serviceEndpoint")
    val serviceEndpoint: String
) {}

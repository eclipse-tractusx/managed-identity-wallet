package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.Serializable

@Serializable
data class VerifiableCredentialParameters(
    @Param(type = ParamType.QUERY)
    @Field(description = "The ID of the Credential as String (URI compatible)", name = "id")
    val id: String,
    @Param(type = ParamType.QUERY)
    @Field(description = "The list of Types", name = "type")
    val type: List<String>,
    @Param(type = ParamType.QUERY)
    @Field(description = "The DID of Issuer as String (URI compatible)", name = "issuer")
    val issuer: String,
    @Param(type = ParamType.QUERY)
    @Field(description = "The DID of holder as String (URI compatible)", name = "holder")
    val holder: String
)

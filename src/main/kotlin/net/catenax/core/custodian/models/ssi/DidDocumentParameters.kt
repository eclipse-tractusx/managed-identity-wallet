package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import kotlinx.serialization.Serializable

@Serializable
data class DidDocumentParameters(
    @Param(type = ParamType.PATH)
    @Field(description = "The DID as String (URI compatible)", name = "did")
    val did: String
)

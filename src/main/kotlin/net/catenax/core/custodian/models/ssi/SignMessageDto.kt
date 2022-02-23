package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.Serializable

@Serializable
data class SignMessageDto(
    @Field(description = "The DID of the signer", name = "did")
    val did: String,
    @Field(description = "The message to sign", name = "message")
    val message: String) { }

package net.catenax.core.custodian.models.ssi

import io.bkbn.kompendium.annotations.Field
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class SignMessageDto(
    @Field(description = "The DID or BPN of the signer", name = "identifier")
    val identifier: String,
    @Field(description = "The message to sign", name = "message")
    val message: String)

@Serializable
data class SignMessageResponseDto(
    val identifier: String,
    val message: String,
    val signedMessageInHex: String,
    val publicKeyBase58: String
)

package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable

@Serializable
data class SignMessageResponseDto(
    val did: String,
    val message: String,
    val signedMessageInHex: String,
    val publicKeyBase58: String
) { }

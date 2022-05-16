package net.catenax.core.managedidentitywallets.models.ssi

import kotlinx.serialization.Serializable

@Serializable
data class DidVerificationMethodDto(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyBase64: String? = null,
    val publicKeyBase58: String? = null,
    val publicKeyHex: String? = null,
    val publicKeyPem: String? = null,
    val publicKeyMultibase: String? = null,
    val publicKeyJwk: PublicKeyJwkDto? = null
)

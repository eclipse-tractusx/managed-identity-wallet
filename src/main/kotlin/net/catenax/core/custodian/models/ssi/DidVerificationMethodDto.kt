@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.catenax.core.custodian.plugins.AnySerializer

@Serializable
class DidVerificationMethodDto(
    val id: String,
    val type: String,
    val controller: String? = null,
    val publicKeyBase64: String? = null,
    val publicKeyBase58: String? = null,
    val publicKeyHex: String? = null,
    val publicKeyPem: String? = null,
    val publicKeyMultibase: String? = null,
    val publicKeyJwk: Map<String, Any>? = null
) {}

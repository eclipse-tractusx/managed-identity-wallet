@file:UseSerializers(AnySerializer::class)

package net.catenax.core.managedidentitywallets.models.ssi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.catenax.core.managedidentitywallets.plugins.AnySerializer

@Serializable
data class PublicKeyJwkDto(
    val kty: String,
    val use: String? = null,
    @SerialName("key_ops")
    val keyOps: List<String>? = null,
    val alg: String? = null,
    val kid: String? = null,
    val crv: String? = null,
    val x: String? = null,
    val y: String? = null,
    val additionalAttributes: Map<String, Any>? = null
)

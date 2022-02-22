@file:UseSerializers(AnySerializer::class)

package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonNames
import net.catenax.core.custodian.plugins.AnySerializer

@Serializable
class VerifiablePresentationDto(
    @JsonNames("@context") val context: List<String>,
    val type: List<String>,
    val holder: String,
    val verifiableCredentials: List<VerifiableCredentialDto>,
    val proof: LdProofDto
) { }

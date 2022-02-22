package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


@Serializable
class VerifiablePresentationRequestDto(
    @JsonNames("@context") val context: List<String>,
    val type: List<String>,
    val holder: String,
    val verifiableCredentialIds: List<String>
) { }

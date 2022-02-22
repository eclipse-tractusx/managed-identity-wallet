package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable

@Serializable
class DidServiceDto(
    val id: String,
    val type: String,
    val serviceEndpoint: String,
) {}

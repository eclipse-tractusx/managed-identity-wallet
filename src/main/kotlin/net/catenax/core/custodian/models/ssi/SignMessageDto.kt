package net.catenax.core.custodian.models.ssi

import kotlinx.serialization.Serializable

@Serializable
data class SignMessageDto(val did: String, val message: String) { }

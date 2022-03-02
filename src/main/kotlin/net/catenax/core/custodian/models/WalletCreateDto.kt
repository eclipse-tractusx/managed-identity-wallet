package net.catenax.core.custodian.models

import net.catenax.core.custodian.plugins.*

import kotlinx.serialization.Serializable

@Serializable
data class WalletCreateDto(
    val bpn: String,
    val name: String,
    val did: String? = null
) {
    init {
        require(bpn.isNotBlank()) { "Field 'bpn' is required not to be blank, but it was blank" }
        require(name.isNotBlank()) { "Field 'name' is required not to be blank, but it was blank" }
    }
}
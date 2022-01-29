package net.catenax.core.custodian.models

import net.catenax.core.custodian.plugins.*

import kotlinx.serialization.Serializable

@Serializable
data class WalletCreateDto(val did: String, @Serializable(with = StringListSerializer::class) val vcs: List<String>) {

    companion object {
        val INVALID: WalletCreateDto = WalletCreateDto("did:bpn:invalid", emptyList<String>())
    }

    init {
        require(did.isNotBlank()) { "Field 'did' is required not to be blank, but it was blank" }
    }
}
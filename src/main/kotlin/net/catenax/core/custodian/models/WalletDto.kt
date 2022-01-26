package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class WalletDto(val did: String, val vcs: List<String>) {

    companion object {
        val INVALID: WalletDto = WalletDto("invalid", emptyList<String>())
    }

    init {
        require(did.isNotBlank()) { "Field 'did' is required not to be blank, but it was blank" }
    }
}
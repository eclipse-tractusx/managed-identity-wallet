package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class Wallet(val did: String, val vcs: List<String>) {
    init {
        require(did.isNotBlank()) { "DID is blank" }
    }
}

val walletStorage = mutableListOf<Wallet>()

package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class Company(val bpn: String, val name: String, val wallet: Wallet) {
    init {
        require(bpn.isNotBlank()) { "BPN is blank" }
        require(name.isNotBlank()) { "Name is blank" }
    }
}

val companyStorage = mutableListOf<Company>()

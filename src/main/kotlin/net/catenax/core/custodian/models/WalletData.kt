package net.catenax.core.custodian.models

import kotlinx.serialization.Serializable

@Serializable
data class WalletData(
    val name: String,
    val bpn: String,
    val did: String,
    var walletId: String,
    var walletKey: String,
    var walletToken: String)
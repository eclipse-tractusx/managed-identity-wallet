package net.catenax.core.custodian.persistances.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object VerifiableCredentials : IntIdTable("verifiablecredentials") {
    val did = varchar("did", 36)
    val content = text("content")
    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var did by VerifiableCredentials.did
    var content by VerifiableCredentials.content
    var walletId by Wallet referencedOn VerifiableCredentials.walletId
}

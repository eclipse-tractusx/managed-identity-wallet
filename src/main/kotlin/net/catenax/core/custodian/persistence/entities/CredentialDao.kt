package net.catenax.core.custodian.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object VerifiableCredentials : IntIdTable("verifiable_credentials") {
    val content = text("content")
    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var content by VerifiableCredentials.content
    var walletId by Wallet referencedOn VerifiableCredentials.walletId
}

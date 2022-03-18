package net.catenax.core.custodian.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object VerifiableCredentials : IntIdTable("verifiable_credentials") {
    val content = text("content")
    val credentialId = varchar("credential_id", 4096).nullable()
    val issuerIdentifier = varchar("issuer_identifier", 4096)
    val holderIdentifier = varchar("holder_identifier", 4096)
    val type = varchar("type", 4096)
    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var content by VerifiableCredentials.content
    var credentialId by VerifiableCredentials.credentialId
    var issuerIdentifier by VerifiableCredentials.issuerIdentifier
    var holderIdentifier by VerifiableCredentials.holderIdentifier
    var type by VerifiableCredentials.type
    var wallet by Wallet referencedOn VerifiableCredentials.walletId
}

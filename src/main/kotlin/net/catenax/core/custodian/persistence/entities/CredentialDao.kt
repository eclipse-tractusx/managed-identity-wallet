package net.catenax.core.custodian.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object VerifiableCredentials : IntIdTable("verifiable_credentials") {
    val content = text("content").uniqueIndex("content")
    val credentialId = varchar("credential_id", 4096).nullable()
    val issuerDid = varchar("issuer_did", 4096)
    val holderDid = varchar("holder_did", 4096)
    val type = varchar("type", 4096)
    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var content by VerifiableCredentials.content
    var credentialId by VerifiableCredentials.credentialId
    var issuerDid by VerifiableCredentials.issuerDid
    var holderDid by VerifiableCredentials.holderDid
    var type by VerifiableCredentials.type
    var wallet by Wallet referencedOn VerifiableCredentials.walletId
}

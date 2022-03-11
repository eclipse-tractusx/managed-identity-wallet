package net.catenax.core.custodian.persistence.entities

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.*

object Wallets : IntIdTable("wallets") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime())

    val name = varchar("name", 127)
    var bpn = varchar("bpn", 36).uniqueIndex("bpn")
    val did = varchar("did", 4096).uniqueIndex("did")

    val privateKey = varchar("privateKey", 4096).uniqueIndex("privateKey")
    val publicKey = varchar("publicKey", 4096).uniqueIndex("publicKey")
}

class Wallet(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Wallet>(Wallets)
    var createdAt by Wallets.createdAt
    var modifiedAt by Wallets.modifiedAt

    var name by Wallets.name
    var bpn by Wallets.bpn
    var did by Wallets.did

    var privateKey by Wallets.privateKey
    var publicKey by Wallets.publicKey
}

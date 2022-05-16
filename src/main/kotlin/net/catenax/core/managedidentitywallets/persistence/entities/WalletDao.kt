package net.catenax.core.managedidentitywallets.persistence.entities

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

    val walletId = varchar("wallet_id", 4096)
    val walletKey = varchar("wallet_key", 4096)
    val walletToken = varchar("wallet_token", 4096)
}

class Wallet(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Wallet>(Wallets)
    var createdAt by Wallets.createdAt
    var modifiedAt by Wallets.modifiedAt

    var name by Wallets.name
    var bpn by Wallets.bpn
    var did by Wallets.did

    var walletId by Wallets.walletId
    var walletKey by Wallets.walletKey
    var walletToken by Wallets.walletToken
}

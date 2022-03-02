package net.catenax.core.custodian.persistances.entities

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.transactions.transaction

import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.KeyPair
import java.util.Base64

import net.catenax.core.custodian.models.*
import org.bitcoinj.core.Base58
import java.time.LocalDateTime

object Wallets : IntIdTable("wallets") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime())

    val name = varchar("name", 127).uniqueIndex("name")
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
    var did by Wallets.did
    var bpn by Wallets.bpn

    var privateKey by Wallets.privateKey
    var publicKey by Wallets.publicKey
}

package net.catenax.core.custodian.entities

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.transactions.transaction

import net.catenax.core.custodian.models.*

object VerifiableCredentials : IntIdTable("verifiablecredentials") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime())
    val did = varchar("did", 36)
    val content = text("content")

    val walletId = reference("wallet_id", Wallets)
}

class VerifiableCredential(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, VerifiableCredential>(VerifiableCredentials)
    var did by VerifiableCredentials.did

    var createdAt by VerifiableCredentials.createdAt
    var modifiedAt by VerifiableCredentials.modifiedAt

    var content by VerifiableCredentials.content

    var walletId by Wallet referencedOn VerifiableCredentials.walletId
}

object Wallets : IntIdTable("wallets") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime())
    val did = varchar("did", 36)
    val name = varchar("name", 127)

    val companyId = reference("company_id", Companies)
}

class Wallet(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Wallet>(Wallets)
    var did by Wallets.did

    var createdAt by Wallets.createdAt
    var modifiedAt by Wallets.modifiedAt

    var name by Wallets.name

    var company by Company referencedOn Wallets.companyId
}

object WalletDao {
    fun getAll() = transaction {
      Wallet.all()
          .toList()
          .map { toObject(it) }
    }

    @Throws(NotFoundException::class)
    fun getWallet(did: String) = Wallet
        .find { Wallets.did eq did }
        .firstOrNull()
        ?: throw NotFoundException("Wallet with did $did not found")

    @Throws(NotFoundException::class)
    fun getWalletForCompany(company: Company) = Wallet
        .find { Wallets.companyId eq company.id }
        .firstOrNull()
        ?: throw NotFoundException("Wallet for company $company not found")

    fun createWallet(c: Company, wallet: WalletDto) = transaction {
        // TODO add VCs
        Wallet.new {
            did = wallet.did
            name = "walletname"
            company = c
        }
    }

    fun toObject(entity: Wallet) = entity.run {
        WalletDto(did, emptyList<String>())
    }

}
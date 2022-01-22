package net.catenax.core.custodian.entities

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.transactions.transaction

import net.catenax.core.custodian.models.*

import java.time.Instant
import java.util.*

object Companies : IntIdTable("companies") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime())
    val bpn = varchar("bpn", 36).uniqueIndex()

    val name = varchar("name", 127)
}

class Company(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Company>(Companies)
    var bpn by Companies.bpn

    var createdAt by Companies.createdAt
    var modifiedAt by Companies.modifiedAt

    var name by Companies.name
}

object CompanyDao {
    fun getAll() = transaction {
      Company.all()
          .toList()
          .map { toObject(it, WalletDao.getWalletForCompany(it)) }
    }

    @Throws(NotFoundException::class)
    fun getCompany(bpn: String) = transaction {
        Company
        .find { Companies.bpn eq bpn }
        .firstOrNull()
        ?: throw NotFoundException("Company with bpn $bpn not found")
    }

    fun toObject(entity: Company, wallet: Wallet) = entity.run {
        CompanyDto(bpn, name, WalletDao.toObject(wallet))
    }

}
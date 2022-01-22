package net.catenax.core.custodian.plugins

import io.ktor.application.*

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

import net.catenax.core.custodian.entities.*

fun Application.configurePersistence() {
    val jdbcUrl = environment.config.property("db.jdbcUrl").getString()
    val jdbcDriver = environment.config.property("db.jdbcDriver").getString()
    Database.connect(jdbcUrl, driver = jdbcDriver)
    transaction {
        // Create missing tables
        SchemaUtils.createMissingTablesAndColumns(Companies, Wallets, VerifiableCredentials)
        commit()
    } 
}

package net.catenax.core.managedidentitywallets.persistence.repositories

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.catenax.core.managedidentitywallets.models.ssi.IssuedVerifiableCredentialRequestDto
import net.catenax.core.managedidentitywallets.models.ssi.VerifiableCredentialDto
import net.catenax.core.managedidentitywallets.persistence.entities.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class CredentialRepository {

    fun deleteCredentialsOfWallet(walletId: Int) {
        VerifiableCredentials.deleteWhere { VerifiableCredentials.walletId eq walletId }
    }

    fun getCredentials(
        issuerIdentifier: String?,
        holderIdentifier: String?,
        type: String?,
        credentialId: String?
    ): List<VerifiableCredentialDto> = transaction {
        val query = VerifiableCredentials.selectAll()
        issuerIdentifier?.let {
            query.andWhere { VerifiableCredentials.issuerDid eq it }
        }
        holderIdentifier?.let {
            query.andWhere { VerifiableCredentials.holderDid eq it }
        }
        type?.let {
            query.andWhere { VerifiableCredentials.type eq it }
        }
        credentialId?.let {
            query.andWhere { VerifiableCredentials.credentialId eq it }
        }
        query.toList().map {
            Json.decodeFromString(it[VerifiableCredentials.content])
        }
    }

    fun storeCredential(
        issuedCredential: IssuedVerifiableCredentialRequestDto,
        credentialAsJson: String,
        typesAsString: String,
        holderWallet: Wallet
    ): VerifiableCredential {
        return VerifiableCredential.new {
            credentialId = issuedCredential.id
            content = credentialAsJson
            issuerDid = issuedCredential.issuer
            holderDid = issuedCredential.credentialSubject["id"] as String
            type = typesAsString
            wallet = holderWallet
        }
    }
}

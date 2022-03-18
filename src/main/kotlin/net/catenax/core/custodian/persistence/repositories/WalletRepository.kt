package net.catenax.core.custodian.persistence.repositories

import net.catenax.core.custodian.models.NotFoundException
import net.catenax.core.custodian.models.WalletData
import net.catenax.core.custodian.models.WalletDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto
import net.catenax.core.custodian.persistence.entities.*
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class WalletRepository {

    fun getAll(): List<Wallet> = transaction { Wallet.all().toList() }

    @Throws(NotFoundException::class)
    fun getWallet(identifier: String): Wallet {
        return Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }
            .firstOrNull()
            ?: throw NotFoundException("Wallet with identifier $identifier not found")
    }

    fun addWallet(wallet: WalletData): Wallet {
        // TODO add VCs: request cx data pool information
        val createdWallet = Wallet.new {
            bpn = wallet.bpn
            name = wallet.name
            did = wallet.did
            walletId = wallet.walletId
            walletKey = wallet.walletKey
            walletToken = wallet.walletToken
            createdAt = LocalDateTime.now()
        }
        return createdWallet
    }

    fun deleteWallet(identifier: String): Boolean {
        getWallet(identifier).delete()
        return true
    }

    fun toObject(entity: Wallet): WalletDto = entity.run {
        WalletDto(name, bpn, did, createdAt, emptyList<VerifiableCredentialDto>())
    }

    fun toWalletCompleteDataObject(entity: Wallet): WalletData = entity.run {
        WalletData( name, bpn, did, walletId, walletKey, walletToken)
    }
}

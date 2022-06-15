package net.catenax.core.managedidentitywallets.persistence.repositories

import net.catenax.core.managedidentitywallets.models.ConflictException
import net.catenax.core.managedidentitywallets.models.NotFoundException
import net.catenax.core.managedidentitywallets.models.WalletExtendedData
import net.catenax.core.managedidentitywallets.models.WalletDto
import net.catenax.core.managedidentitywallets.models.ssi.VerifiableCredentialDto
import net.catenax.core.managedidentitywallets.persistence.entities.*
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

    @Throws(ConflictException::class)
    fun checkWalletAlreadyExists(identifier: String) {
         if (!Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }.empty()) {
             throw ConflictException("Wallet with identifier $identifier already exists!")
         }
    }

    fun addWallet(wallet: WalletExtendedData): Wallet {
        // no VCs are added in this step, they will come in through the business partner data service
        return Wallet.new {
            bpn = wallet.bpn
            name = wallet.name
            did = wallet.did
            walletId = wallet.walletId
            walletKey = wallet.walletKey
            walletToken = wallet.walletToken
            createdAt = LocalDateTime.now()
        }
    }

    fun deleteWallet(identifier: String): Boolean {
        getWallet(identifier).delete()
        return true
    }

    fun toObject(entity: Wallet): WalletDto = entity.run {
        WalletDto(name, bpn, did, null, createdAt, emptyList<VerifiableCredentialDto>().toMutableList())
    }

    fun toWalletCompleteDataObject(entity: Wallet): WalletExtendedData = entity.run {
        WalletExtendedData(id.value, name, bpn, did, walletId, walletKey, walletToken)
    }
}

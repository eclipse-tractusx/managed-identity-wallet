package net.catenax.core.custodian.services

import net.catenax.core.custodian.models.WalletCreateDto
import net.catenax.core.custodian.models.WalletDto
import net.catenax.core.custodian.persistence.repositories.WalletRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class WalletService(private val walletRepository: WalletRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    fun getWallet(identifier: String): WalletDto {
        log.debug("Get Wallet with identifier $identifier")
        return transaction {
            val extractedWallet = walletRepository.getWallet(identifier)
            walletRepository.toObject(extractedWallet)
        }
    }

    fun getAll(): List<WalletDto> {
        log.debug("Get All Wallets")
        return transaction {
            val listOfWallets = walletRepository.getAll()
            listOfWallets.map { walletRepository.toObject(it) }
        }
    }

    fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
        log.debug("Add a new Wallet with bpn ${walletCreateDto.bpn}")
        return transaction {
            val createdWallet = walletRepository.addWallet(walletCreateDto)
            walletRepository.toObject(createdWallet)
        }
    }

    fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        return transaction {
            walletRepository.deleteWallet(identifier)
        }
    }
}

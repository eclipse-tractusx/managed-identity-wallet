package net.catenax.core.custodian.services

import net.catenax.core.custodian.models.WalletCreateDto
import net.catenax.core.custodian.models.WalletDto
import net.catenax.core.custodian.persistence.repositories.WalletRepository
import org.slf4j.LoggerFactory

class WalletService(private val walletRepository: WalletRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    fun getWallet(identifier: String): WalletDto {
        log.debug("Get Wallet with identifier $identifier")
        val wallet = walletRepository.getWallet(identifier)
        return walletRepository.toObject(wallet)
    }

    fun getAll(): List<WalletDto> {
        log.debug("Get All Wallets")
        val wallets = walletRepository.getAll()
        return wallets.map { walletRepository.toObject(it) }
    }

    fun createWallet(walletCreateDto: WalletCreateDto): WalletDto {
        log.debug("Add a new Wallet with bpn ${walletCreateDto.bpn}")
        val createdWallet = walletRepository.addWallet(walletCreateDto)
        return walletRepository.toObject(createdWallet)
    }

    fun deleteWallet(identifier: String): Boolean {
        log.debug("Delete Wallet with identifier $identifier")
        return walletRepository.deleteWallet(identifier)
    }
}

package net.catenax.core.custodian.persistence.repositories

import net.catenax.core.custodian.models.NotFoundException
import net.catenax.core.custodian.models.WalletCreateDto
import net.catenax.core.custodian.models.WalletDto
import net.catenax.core.custodian.models.ssi.VerifiableCredentialDto
import net.catenax.core.custodian.persistence.entities.*
import org.bitcoinj.core.Base58
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.LocalDateTime
import java.util.*

class WalletRepository {

    fun getAll(): List<Wallet> = transaction { Wallet.all().toList() }

    @Throws(NotFoundException::class)
    fun getWallet(identifier: String): Wallet = transaction {
        Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }
            .firstOrNull()
            ?: throw NotFoundException("Wallet with identifier $identifier not found")
    }

    fun addWallet(wallet: WalletCreateDto): Wallet = transaction {
        // TODO add VCs: request cx data pool information
        val kpg = KeyPairGenerator.getInstance("EC")
        val params = ECGenParameterSpec("secp256r1")
        kpg.initialize(params);
        val kp = kpg.generateKeyPair()

        Wallet.new {
            bpn = wallet.bpn
            name = wallet.name
            did = "did:example:" + Base58.encode(kp.public.encoded).toString()
            createdAt = LocalDateTime.now()
            privateKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded())
            publicKey = Base58.encode(kp.public.encoded).toString()
        }
    }

    fun deleteWallet(identifier: String): Boolean = transaction {
        val wallet = getWallet(identifier)
        wallet.delete()
        true
    }

    fun toObject(entity: Wallet): WalletDto = entity.run {
        WalletDto(name, bpn, did, createdAt, publicKey, emptyList<VerifiableCredentialDto>())
    }
}

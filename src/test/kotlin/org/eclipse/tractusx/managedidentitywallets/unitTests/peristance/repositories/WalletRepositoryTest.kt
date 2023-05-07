package org.eclipse.tractusx.managedidentitywallets.unitTests.peristance.repositories

import junit.framework.TestCase.assertEquals
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallet
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallets
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.unitTests.utils.InMemoryDatabase
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class WalletRepositoryTest: InMemoryDatabase("wallet_db_test", arrayOf(Wallets)) {
    private val walletRepo = WalletRepository()

    @Test
    fun `Can create a new Wallet in the DB`() {
        val toExpect = Wallet.new {
            createdAt = LocalDateTime.now()
            modifiedAt = LocalDateTime.now()
            modifiedFrom = "BPN1234"
            name = "TestWallet"
            bpn = "BPN1234"
            did = "did:web:example.com"
            didDocument = "{}"

            active = true
            authority = false

            algorithm = "ED25519"
        }
        val actual = walletRepo.getWallet("BPN1234")
        assertEquals(toExpect, actual)
    }
}
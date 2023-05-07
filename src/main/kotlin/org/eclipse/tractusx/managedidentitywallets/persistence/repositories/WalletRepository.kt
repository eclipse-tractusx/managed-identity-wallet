/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets.persistence.repositories

import org.eclipse.tractusx.managedidentitywallets.models.ConflictException
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.models.WalletExtendedData
import org.eclipse.tractusx.managedidentitywallets.models.ssi.VerifiableCredentialDto
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallet
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class WalletRepository {

    fun getAll(): List<Wallet> = transaction { Wallet.all().toList() }

    @Throws(NotFoundException::class)
    fun getWallet(bpn: String): Wallet {
        return transaction {
            Wallet.find {
                (Wallets.bpn eq bpn)
            }.firstOrNull() ?: throw NotFoundException("Wallet with given bpn not found")
        }
    }

    fun getWalletOrNull(bpn: String): Wallet? {
        return transaction {
            Wallet.find {
                (Wallets.bpn eq bpn)
            }.firstOrNull()
        }
    }

    @Throws(ConflictException::class)
    fun checkWalletAlreadyExists(identifier: String) {
        if (!Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }.empty()) {
            throw ConflictException("Wallet with given identifier already exists!")
        }
    }

    fun isWalletExists(identifier: String): Boolean {
        return transaction {
            !Wallet.find { (Wallets.did eq identifier) or (Wallets.bpn eq identifier) }.empty()
        }
    }

    fun addWallet(wallet: WalletExtendedData): Wallet {
        // no VCs are added in this step, they will come in through the business partner data service
        return Wallet.new {
            bpn = wallet.bpn
            name = wallet.name
            did = wallet.did
            createdAt = LocalDateTime.now()
            modifiedAt = LocalDateTime.now()
            modifiedFrom = wallet.bpn
            didDocument = ""
            active = true
            authority = false
            algorithm = "ED25519"
        }
    }

    @Throws(NotFoundException::class)
    fun deleteWallet(bpn: String): Boolean {
        getWallet(bpn).delete()
        return true
    }

    fun toObject(entity: Wallet): WalletDto = entity.run {
        WalletDto(
            name = name,
            bpn = bpn,
            did = did,
            createdAt = createdAt,
            vcs = emptyList<VerifiableCredentialDto>().toMutableList()
        )
    }

    fun toWalletCompleteDataObject(entity: Wallet): WalletExtendedData = entity.run {
        WalletExtendedData(
            id.value,
            name,
            bpn,
            did
        )
    }
}

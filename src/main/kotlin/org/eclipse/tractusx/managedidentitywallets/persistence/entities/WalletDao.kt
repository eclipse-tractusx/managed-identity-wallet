/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Wallets : IntIdTable("wallets") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime)

    val name = varchar("name", 127)
    var bpn = varchar("bpn", 36).uniqueIndex("bpn")
    val did = varchar("did", 4096).uniqueIndex("did")

    val walletId = varchar("wallet_id", 4096).nullable()
    val walletKey = varchar("wallet_key", 4096).nullable()
    val walletToken = varchar("wallet_token", 4096).nullable()

    val revocationListName = varchar("revocation_list_name", 4096).nullable()
    val pendingMembershipIssuance = bool("pending_membership_issuance").default(false)
}

class Wallet(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Wallet>(Wallets)
    var createdAt by Wallets.createdAt
    var modifiedAt by Wallets.modifiedAt

    var name by Wallets.name
    var bpn by Wallets.bpn
    var did by Wallets.did

    var walletId by Wallets.walletId
    var walletKey by Wallets.walletKey
    var walletToken by Wallets.walletToken

    var revocationListName by Wallets.revocationListName

    var pendingMembershipIssuance by Wallets.pendingMembershipIssuance
}

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

package org.eclipse.tractusx.managedidentitywallets.persistence.entities

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object KeyReferences : IntIdTable("key_references") {
    val bpn = reference("bpn", Wallets.bpn)
    val vaultAccestoken = varchar("vault_accestoken", 4096)
    val referenceKey = varchar("reference_key", 36).uniqueIndex("bpn")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val modifiedAt = datetime("modified_at").defaultExpression(CurrentDateTime)
    val modifiedFrom = varchar("modified_from", 36)
}

class KeyReference(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, KeyReference>(KeyReferences)
    var vaultAccestoken by KeyReferences.vaultAccestoken
    var referenceKey by KeyReferences.referenceKey
    var createdAt by KeyReferences.createdAt
    var modifiedAt by KeyReferences.modifiedAt
    var modifiedFrom by KeyReferences.modifiedFrom
    var bpn by KeyReferences.bpn
}

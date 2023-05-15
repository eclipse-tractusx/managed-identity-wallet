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

package org.eclipse.tractusx.managedidentitywallets.plugins

import io.ktor.application.*
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.KeyReferences
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.VerifiableCredentials
import org.eclipse.tractusx.managedidentitywallets.persistence.entities.Wallets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configurePersistence() {
    val jdbcUrl = environment.config.property("db.jdbcUrl").getString()
    val jdbcDriver = environment.config.property("db.jdbcDriver").getString()
    Database.connect(jdbcUrl, driver = jdbcDriver)
    transaction {
        // addLogger(StdOutSqlLogger)

        // Create missing tables
        SchemaUtils.createMissingTablesAndColumns(
            Wallets,
            VerifiableCredentials,
            KeyReferences
        )
        commit()
    }
}

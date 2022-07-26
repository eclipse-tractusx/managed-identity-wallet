/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the CatenaX (ng) GitHub Organisation
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
import kotlinx.coroutines.runBlocking
import net.kiberion.ktor_scheduler.Scheduler
import net.kiberion.ktor_scheduler.recurringJob
import net.kiberion.ktor_scheduler.schedule
import org.eclipse.tractusx.managedidentitywallets.Services
import org.jobrunr.scheduling.cron.Cron
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource


fun Application.configureJobs() {
    val jdbcUrl = environment.config.property("db.jdbcUrl").getString()
    val pullDataAtHour = environment.config.property("bpdm.pullDataAtHour").getString().toInt()

    install(Scheduler) {
        storageProvider = PostgresStorageProvider(initDatabase(jdbcUrl))
        threads = 5
    }

    schedule {
        recurringJob("bpdm-update", Cron.daily(pullDataAtHour)) {
            runJobPayload()
        }
    }
}

fun initDatabase(jdbcUrl: String): DataSource {
    val dataSource = PGSimpleDataSource()
    dataSource.setUrl(jdbcUrl)
    return dataSource
}

fun runJobPayload() {
    runBlocking {
        Services.businessPartnerDataService.pullDataAndUpdateCatenaXCredentialsAsync()
    }
}

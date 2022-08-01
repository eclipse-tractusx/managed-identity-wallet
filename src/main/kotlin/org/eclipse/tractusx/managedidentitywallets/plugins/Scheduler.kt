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

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import io.ktor.application.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.Services
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteDataSource
import java.sql.DriverManager
import java.time.Duration
import javax.sql.DataSource

fun Application.configureJobs() {

    val jdbcUrl = environment.config.property("db.jdbcUrl").getString()
    val pullDataAtHour = environment.config.property("bpdm.pullDataAtHour").getString().toInt()

    val bpdmUpdate: RecurringTask<Void> = Tasks.recurring("bpdm-update",
        // Spring Scheduled tasks (second, minute, hour, day of month, month, day(s) of week)
        Schedules.cron("0 0 $pullDataAtHour * * *"))
        .execute { inst, ctx ->
            runJobPayload()
        }

    val scheduler: Scheduler = Scheduler
        .create(initDatabase(jdbcUrl))
        .startTasks(bpdmUpdate)
        .pollingInterval(Duration.ofHours(1))
        .registerShutdownHook()
        .threads(3)
        .build()

    scheduler.start()
}

fun initDatabase(jdbcUrl: String): DataSource {
    if (jdbcUrl.startsWith("jdbc:sqlite")) {
        val dataSource = SQLiteDataSource()
        dataSource.setUrl(jdbcUrl)

        // just a keepAliveConnection
        DriverManager.getConnection(jdbcUrl)

        return dataSource
    } else {
        val dataSource = PGSimpleDataSource()
        dataSource.setUrl(jdbcUrl)
        return dataSource
    }
}

fun runJobPayload() {
    runBlocking {
        Services.businessPartnerDataService.pullDataAndUpdateCatenaXCredentialsAsync()
    }
}

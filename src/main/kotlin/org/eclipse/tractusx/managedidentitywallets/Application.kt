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

package org.eclipse.tractusx.managedidentitywallets

import io.ktor.application.*
import io.ktor.features.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.models.ServicesHttpClientConfig
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.ConnectionRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WebhookRepository
import org.eclipse.tractusx.managedidentitywallets.plugins.configureJobs
import org.eclipse.tractusx.managedidentitywallets.plugins.configureOpenAPI
import org.eclipse.tractusx.managedidentitywallets.plugins.configurePersistence
import org.eclipse.tractusx.managedidentitywallets.plugins.configureRouting
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSecurity
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSerialization
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSockets
import org.eclipse.tractusx.managedidentitywallets.plugins.configureStatusPages
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import org.eclipse.tractusx.managedidentitywallets.services.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object Services {
    lateinit var walletService: IWalletService
    lateinit var utilsService: UtilsService
    lateinit var revocationService: IRevocationService
}

fun Application.module(testing: Boolean = false) {

    val log = LoggerFactory.getLogger(this::class.java)
    if (testing) {
        log.info("Starting in testing mode...")
    }

    environment.monitor.subscribe(ApplicationStarted, ::onStarted)

    configureSockets()
    configureSerialization()

    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
    }
    
    // for debugging
    install(CallLogging)

    // Installs the Kompendium Plugin and sets up baseline server metadata
    configureOpenAPI()

    configureStatusPages()

    configureSecurity()

    val walletRepository = WalletRepository()
    val credRepository = CredentialRepository()
    val connectionRepository = ConnectionRepository()
    val webhookRepository = WebhookRepository()

    val networkIdentifier = environment.config.property("acapy.networkIdentifier").getString()
    val utilsService = UtilsService(networkIdentifier = networkIdentifier)

    val baseWalletBpn = environment.config.property("wallet.baseWalletBpn").getString()
    val allowlistDidsAsString = environment.config.property("wallet.allowlistDids").getString()
    val allowlistDids = if (allowlistDidsAsString.isBlank()) {
        emptyList()
    } else {
        allowlistDidsAsString.split(",")
    }

    val servicesHttpClientLogLevel = environment.config.property("logging.logLevelServicesCalls").getString()
    val httpClientWalletServiceConfig = ServicesHttpClientConfig(
        servicesHttpClientLogLevel,
        environment.config.property("httpTimeout.walletServiceRequestTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.walletServiceConnectTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.walletServiceSocketTimeoutMillis").getString().toLong()
    )
    val httpClientBPDServiceConfig = ServicesHttpClientConfig(
        servicesHttpClientLogLevel,
        environment.config.property("httpTimeout.bpdServiceRequestTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.bpdServiceConnectTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.bpdServiceSocketTimeoutMillis").getString().toLong()
    )
    val httpClientRevocationServiceConfig = ServicesHttpClientConfig(
        servicesHttpClientLogLevel,
        environment.config.property("httpTimeout.revocationServiceRequestTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.revocationServiceConnectTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.revocationServiceSocketTimeoutMillis").getString().toLong()
    )

    val httpClientWebhookServiceConfig = ServicesHttpClientConfig(
        servicesHttpClientLogLevel,
        environment.config.property("httpTimeout.webhookServiceRequestTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.webhookServiceConnectTimeoutMillis").getString().toLong(),
        environment.config.property("httpTimeout.webhookServiceSocketTimeoutMillis").getString().toLong()
    )

    val revocationUrl = environment.config.property("revocation.baseUrl").getString()
    val revocationService = IRevocationService.createRevocationService(revocationUrl, httpClientRevocationServiceConfig)
    val walletService = AgentWalletServiceImpl() //TODO
    val membershipOrganisation = environment.config.property("wallet.membershipOrganisation").getString()

    Services.walletService = walletService
    Services.utilsService = utilsService
    Services.revocationService = revocationService

    configureRouting()

    appRoutes(walletService, revocationService, utilsService)
    configurePersistence()
}

// Should be changed to https://api.ktor.io/ktor-server/ktor-server-core/io.ktor.server.application/-server-ready.html
// when the application is updated to Ktor 2.x
private fun onStarted(app: Application) {
    val bpnOfBaseWallet = app.environment.config.property("wallet.baseWalletBpn").getString()
    val nameOfBaseWallet = app.environment.config.property("wallet.baseWalletName").getString()
    runBlocking {
        Services.walletService.onInitAddAuthorityWallet(
            bpn = bpnOfBaseWallet,
            name = nameOfBaseWallet
        )
    }

    // the revocation service that is triggered by the scheduler has a callback function to the application.
    app.configureJobs()
}

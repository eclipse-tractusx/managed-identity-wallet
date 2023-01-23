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

package org.eclipse.tractusx.managedidentitywallets

import io.ktor.application.*
import io.ktor.features.*
import kotlinx.coroutines.runBlocking
import org.eclipse.tractusx.managedidentitywallets.models.BPDMConfig
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
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
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object Services {
    lateinit var businessPartnerDataService: IBusinessPartnerDataService
    lateinit var walletService: IWalletService
    lateinit var utilsService: UtilsService
    lateinit var revocationService: IRevocationService
    lateinit var webhookService: IWebhookService
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
    val acaPyConfig = WalletAndAcaPyConfig(
        networkIdentifier = networkIdentifier,
        baseWalletBpn = baseWalletBpn,
        baseWalletDID = utilsService.getDidMethodPrefixWithNetworkIdentifier() +
                environment.config.property("wallet.baseWalletShortDid").getString(),
        baseWalletVerkey = environment.config.property("wallet.baseWalletVerkey").getString(),
        apiAdminUrl = environment.config.property("acapy.apiAdminUrl").getString(),
        adminApiKey = environment.config.property("acapy.adminApiKey").getString(),
        baseWalletAdminUrl = environment.config.property("acapy.baseWalletApiAdminUrl").getString(),
        baseWalletAdminApiKey = environment.config.property("acapy.baseWalletAdminApiKey").getString(),
    )
    val revocationUrl = environment.config.property("revocation.baseUrl").getString()
    val revocationService = IRevocationService.createRevocationService(revocationUrl)
    val webhookService = IWebhookService.createWebhookService(webhookRepository)
    val walletService = IWalletService.createWithAcaPyService(
        acaPyConfig,
        walletRepository,
        credRepository,
        utilsService,
        revocationService,
        webhookService,
        connectionRepository
    )
    val bpdmConfig = BPDMConfig(
        url = environment.config.property("bpdm.datapoolUrl").getString(),
        tokenUrl = environment.config.property("bpdm.authUrl").getString(),
        clientId = environment.config.property("bpdm.clientId").getString(),
        clientSecret = environment.config.property("bpdm.clientSecret").getString(),
        scope = environment.config.property("bpdm.scope").getString(),
        grantType = environment.config.property("bpdm.grantType").getString()
    )
    val businessPartnerDataService = IBusinessPartnerDataService.createBusinessPartnerDataService(
        walletService,
        bpdmConfig)
    Services.businessPartnerDataService = businessPartnerDataService
    Services.walletService = walletService
    Services.utilsService = utilsService
    Services.revocationService = revocationService
    Services.webhookService = webhookService

    configureRouting(walletService)

    appRoutes(walletService, businessPartnerDataService, revocationService, webhookService, utilsService)
    configurePersistence()
}

// Should be changed to https://api.ktor.io/ktor-server/ktor-server-core/io.ktor.server.application/-server-ready.html
// when the application is updated to Ktor 2.x
private fun onStarted(app: Application) {
    val bpnOfBaseWallet = app.environment.config.property("wallet.baseWalletBpn").getString()
    val didOfBaseWallet = Services.utilsService.getDidMethodPrefixWithNetworkIdentifier() +
            app.environment.config.property("wallet.baseWalletShortDid").getString()
    val veykeyOfBaseWallet = app.environment.config.property("wallet.baseWalletVerkey").getString()
    val nameOfBaseWallet = app.environment.config.property("wallet.baseWalletName").getString()
    runBlocking {
        Services.walletService.initBaseWalletAndSubscribeForAriesWS(
            bpn = bpnOfBaseWallet,
            did =  didOfBaseWallet,
            verkey = veykeyOfBaseWallet,
            name = nameOfBaseWallet
        )
    }

    // the revocation service that is triggered by the scheduler has a callback function to the application.
    app.configureJobs()
}

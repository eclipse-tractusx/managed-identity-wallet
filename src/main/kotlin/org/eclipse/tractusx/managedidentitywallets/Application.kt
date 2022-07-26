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

package org.eclipse.tractusx.managedidentitywallets

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.acapy.WalletAndAcaPyConfig
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.CredentialRepository
import org.eclipse.tractusx.managedidentitywallets.persistence.repositories.WalletRepository

import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import org.eclipse.tractusx.managedidentitywallets.services.BusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.WalletService

import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object Services {
    lateinit var businessPartnerDataService: BusinessPartnerDataService
    lateinit var walletService: WalletService
}

fun Application.module(testing: Boolean = false) {

    val log = LoggerFactory.getLogger(this::class.java)
    if (testing) {
        log.info("Starting in testing mode...")
    }

    configureSockets()
    configureSerialization()

    install(DefaultHeaders)
    
    // for debugging
    install(CallLogging)

    // Installs the Kompendium Plugin and sets up baseline server metadata
    configureOpenAPI()

    install(StatusPages) {
        exception<BadRequestException> { cause ->
            call.respond(HttpStatusCode.BadRequest, ExceptionResponse(cause.message!!))
        }
        exception<UnprocessableEntityException> { cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, ExceptionResponse(cause.message!!))
        }
        exception<ForbiddenException> { cause ->
            call.respond(HttpStatusCode.Forbidden, ExceptionResponse(cause.message!!))
        }
        exception<NotImplementedException> { cause ->
            call.respond(HttpStatusCode.NotImplemented, ExceptionResponse(cause.message!!))
        }
        exception<NotFoundException> { cause ->
            call.respond(HttpStatusCode.NotFound, ExceptionResponse(cause.message!!))
        }
        exception<ConflictException> { cause ->
            call.respond(HttpStatusCode.Conflict, ExceptionResponse(cause.message!!))
        }
    }

    configureSecurity()

    val walletRepository = WalletRepository()
    val credRepository = CredentialRepository()
    val acaPyConfig = WalletAndAcaPyConfig(
        apiAdminUrl = environment.config.property("acapy.apiAdminUrl").getString(),
        networkIdentifier = environment.config.property("acapy.networkIdentifier").getString(),
        adminApiKey = environment.config.property("acapy.adminApiKey").getString(),
        baseWalletBpn = environment.config.property("wallet.baseWalletBpn").getString()
    )
    val walletService = WalletService.createWithAcaPyService(acaPyConfig, walletRepository, credRepository)
    val bpdmConfig = BPDMConfig(
        url = environment.config.property("bpdm.datapoolUrl").getString(),
        tokenUrl = environment.config.property("bpdm.authUrl").getString(),
        clientId = environment.config.property("bpdm.clientId").getString(),
        clientSecret = environment.config.property("bpdm.clientSecret").getString(),
        scope = environment.config.property("bpdm.scope").getString(),
        grantType = environment.config.property("bpdm.grantType").getString()
    )
    val businessPartnerDataService = BusinessPartnerDataService.createBusinessPartnerDataService(walletService,
        bpdmConfig)
    Services.businessPartnerDataService = businessPartnerDataService
    Services.walletService = walletService
    configureRouting(walletService)

    appRoutes(walletService, businessPartnerDataService)
    configurePersistence()

    configureJobs()
}

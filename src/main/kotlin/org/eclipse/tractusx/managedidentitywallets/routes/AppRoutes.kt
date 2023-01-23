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

package org.eclipse.tractusx.managedidentitywallets.routes

import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.semanticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.models.ssi.ListCredentialRequestData
import org.eclipse.tractusx.managedidentitywallets.models.syntacticallyInvalidInputException
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService
import org.eclipse.tractusx.managedidentitywallets.services.IRevocationService
import org.eclipse.tractusx.managedidentitywallets.services.IWalletService
import org.eclipse.tractusx.managedidentitywallets.services.IWebhookService
import org.eclipse.tractusx.managedidentitywallets.services.ManagedWalletsAriesEventHandler
import org.eclipse.tractusx.managedidentitywallets.services.UtilsService

fun Application.appRoutes(
    walletService: IWalletService,
    businessPartnerDataService: IBusinessPartnerDataService,
    revocationService: IRevocationService,
    webhookService: IWebhookService,
    utilsService: UtilsService
) {

    routing {
        route("/api") {

            walletRoutes(walletService)
            businessPartnerDataRoutes(businessPartnerDataService)
            didDocRoutes(walletService)
            vcRoutes(walletService, revocationService, utilsService)
            vpRoutes(walletService)

        }

        // Used by the revocation service to issue Status-List Credential
        route("/list-credential/{profileName}/issue") {
            notarizedPost(
                PostInfo<Unit, ListCredentialRequestData, String>(
                    summary = "Issue a List Status credential",
                    description = "This endpoint is called by the revocation service to issue a list status credential for a given profileName",
                    parameterExamples = setOf(
                        ParameterExample("profileName", "profileName", "Ae49DuXZy2PLBjSL9W2V2i"),
                    ),
                    requestInfo = RequestInfo(
                        description = "The subject of the status list credential",
                        examples = listCredentialRequestData
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Created,
                        description = "The created verifiable credential",
                        examples = mapOf("demo" to "credential-as-string")
                    ),
                    canThrow = setOf(
                        semanticallyInvalidInputException, syntacticallyInvalidInputException
                    ),
                    tags = setOf("VerifiableCredentials")
                )
            ) {
                val profileName =
                    call.parameters["profileName"] ?: throw BadRequestException("Missing or malformed profileName")
                val listCredentialRequestData = call.receive<ListCredentialRequestData>()
                val verifiableCredentialDto =
                    walletService.issueStatusListCredential(profileName, listCredentialRequestData)
                call.respond(HttpStatusCode.Created, Json.encodeToString(verifiableCredentialDto))
            }
        }


        route("/webhook/topic/{topic}/") {
            notarizedPost(
                PostInfo<Unit, Any, String>(
                    summary = "Webhook to receive messages from Acapy",
                    description = "",
                    parameterExamples = setOf(
                        ParameterExample("topic", "topic", "connections"),
                    ),
                    requestInfo = RequestInfo(
                        description = "the object related to the topic",
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.OK,
                        description = "The webhook endpoint is triggered successfully"
                    ),
                    tags = setOf("Webhook")
                )
            ) {
                val walletId: String = call.request.headers["x-wallet-id"]
                    ?: throw throw BadRequestException("Missing or malformed walletId")
                val topic = call.parameters["topic"] ?: throw BadRequestException("Missing or malformed topic")
                val managedWalletHandler = ManagedWalletsAriesEventHandler(
                    walletService,
                    revocationService,
                    webhookService,
                    utilsService
                )
                val payload = call.receiveText()
                managedWalletHandler.handleEvent(walletId, topic, payload)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

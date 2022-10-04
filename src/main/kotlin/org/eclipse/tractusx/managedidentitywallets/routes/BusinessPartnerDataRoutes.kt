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

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.ParameterExample
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService

fun Route.businessPartnerDataRoutes(businessPartnerDataService: IBusinessPartnerDataService) {

    route("/businessPartnerDataRefresh") {

        notarizedAuthenticate(AuthorizationHandler.JWT_AUTH_TOKEN) {
            notarizedPost(
                PostInfo<BusinessPartnerDataRefreshParameters, Unit, String>(
                    summary = "Pull business partner data from BPDM and issue or update verifiable credentials",
                    description = "Permission: " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLETS)}** OR " +
                        "**${AuthorizationHandler.getPermissionOfRole(AuthorizationHandler.ROLE_UPDATE_WALLET)}** " +
                            "(The BPN of wallet to update must equal BPN of caller) \n" +
                        "\nPull business partner data from BPDM and issue" +
                        "or update related verifiable credentials. " +
                        "To update a specific wallet give its identifier as a query parameter.",
                    parameterExamples = setOf(
                        ParameterExample("identifier", "did", "did:example:0123"),
                        ParameterExample("identifier", "bpn", "bpn123"),
                    ),
                    requestInfo = null,
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Accepted,
                        description = "Empty response body"
                    ),
                    canThrow = setOf(forbiddenException, unauthorizedException),
                    tags = setOf("BusinessPartnerData")
                )
            ) {
                AuthorizationHandler.checkHasRightsToUpdateWallet(call, call.request.queryParameters["identifier"])

                businessPartnerDataService.pullDataAndUpdateCatenaXCredentialsAsync(
                    call.request.queryParameters["identifier"]
                )
                return@notarizedPost call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}

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

package org.eclipse.tractusx.managedidentitywallets.routes

import io.bkbn.kompendium.auth.Notarized.notarizedAuthenticate
import io.bkbn.kompendium.core.Notarized.notarizedPost
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.plugins.AuthConstants
import org.eclipse.tractusx.managedidentitywallets.services.BusinessPartnerDataService

fun Route.businessPartnerDataRoutes(businessPartnerDataService: BusinessPartnerDataService) {

    route("/businessPartnerData") {

        notarizedAuthenticate(AuthConstants.JWT_AUTH_UPDATE) {
            notarizedPost(
                PostInfo<Unit, BusinessPartnerDataUpdateRequestDto, String>(
                    summary = "Update business partner data in the corresponding wallet",
                    description = "Create or update data associated with a given bpn in the corresponding wallet by creating or updating verifiable credentials",
                    requestInfo = RequestInfo(
                        description = "The input data to use for the update",
                        examples = dataUpdateRequestDtoExample
                    ),
                    responseInfo = ResponseInfo(
                        status = HttpStatusCode.Accepted,
                        description = "Empty response body"
                    ),
                    canThrow = setOf(semanticallyInvalidInputException),
                    tags = setOf("BusinessPartnerData"),
                    securitySchemes = setOf(AuthConstants.JWT_AUTH_UPDATE.name)
                )
            ) {
                val dataUpdateRequestDto = call.receive<BusinessPartnerDataUpdateRequestDto>()
                businessPartnerDataService.issueAndUpdateCatenaXCredentials(dataUpdateRequestDto)
                call.respond(HttpStatusCode.Accepted)
            }
        }

    }
}

val dataUpdateRequestDtoExample = mapOf(
    "demo" to BusinessPartnerDataUpdateRequestDto(
        bpn = "BPNL000000000001",
        identifiers = listOf(
          IdentifierDto(
            uuid = "089e828d-01ed-4d3e-ab1e-cccca26814b3",
            value = "BPNL000000000001",
            type = TypeKeyNameUrlDto(
              technicalKey = "BPN",
              name = "Business Partner Number",
              url = ""
            ),
            issuingBody = TypeKeyNameUrlDto(
              technicalKey = "CATENAX",
              name = "Catena-X",
              url = ""
            ),
            status = TypeKeyNameDto(
              technicalKey = "UNKNOWN",
              name = "Unknown"
            )
          )
        ),
        names = listOf(
          ExtendedMultiPurposeDto(
            uuid = "de3f3db6-e337-436b-a4e0-fc7d17e8af89",
            value = "German Car Company",
            shortName = "GCC",
            type = TypeKeyNameUrlDto(
              technicalKey = "REGISTERED",
              name = "The main name under which a business is officially registered in a country's business register.",
              url = ""
            ),
            language = TypeKeyNameDto(
              technicalKey = "undefined",
              name = "Undefined"
            )
          )
        ),
        legalForm = LegalFormDto(
          technicalKey = "DE_AG",
          name = "Aktiengesellschaft",
          url = "",
          mainAbbreviation = "AG",
          language = TypeKeyNameDto(
            technicalKey = "de",
            name = "German"
          ),
          categories = listOf(
            TypeNameUrlDto(
              name = "AG",
              url = ""
            )
          )
        ),
        status = null,
        addresses = listOf(
            AddressDto(
                uuid = "16701107-9559-4fdf-b1c1-8c98799d779d",
                version = AddressVersion(
                    characterSet = TypeKeyNameDto(
                        technicalKey = "WESTERN_LATIN_STANDARD",
                        name = "Western Latin Standard (ISO 8859-1; Latin-1)"
                    ),
                    language = TypeKeyNameDto(
                        technicalKey = "en",
                        name = "English"
                    )
                ),
                careOf = null,
                contexts = emptyList(),
                country = TypeKeyNameDto(
                    technicalKey = "DE",
                    name = "Germany"
                ),
                administrativeAreas = listOf(
                    ExtendedMultiPurposeDto(
                        uuid = "cc6de665-f8eb-45ed-b2bd-6caa28fa8368",
                        value = "Bavaria",
                        shortName = "BY",
                        fipsCode = "GM02",
                        type = TypeKeyNameUrlDto(
                            technicalKey = "REGION",
                            name = "Region",
                            url = ""
                        ),
                        language = TypeKeyNameDto(
                            technicalKey = "en",
                            name = "English"
                        )
                    )
                ),
                postCodes = listOf(
                    PostCode(
                        uuid = "8a02b3d0-de1e-49a5-9528-cfde2d5273ed",
                        value ="80807",
                        type= TypeKeyNameUrlDto(
                            technicalKey = "REGULAR",
                            name = "Regular",
                            url = ""
                        )
                    )
                ),
                localities = listOf(
                    ExtendedMultiPurposeDto(
                        uuid= "2cd18685-fac9-49f4-a63b-322b28f7dc9a",
                        value = "Munich",
                        shortName= "M",
                        type = TypeKeyNameUrlDto(
                            technicalKey= "CITY",
                            name = "City",
                            url = ""
                        ),
                        language = TypeKeyNameDto(
                            technicalKey= "en",
                            name = "English"
                        )
                    )
                ),
                thoroughfares = listOf(
                    ExtendedMultiPurposeDto(
                        uuid= "0c491424-b2bc-44cf-9d14-71cbe513423f",
                        value= "Muenchner Straße 34",
                        name =  "Muenchner Straße",
                        shortName =  null,
                        number = "34",
                        direction= null,
                        type = TypeKeyNameUrlDto(
                            technicalKey = "STREET",
                            name = "Street",
                            url = ""
                        ),
                        language = TypeKeyNameDto(
                            technicalKey = "en",
                            name = "English"
                        )
                    )
                ),
                premises = listOf(),
                postalDeliveryPoints = listOf(),
                geographicCoordinates = null,
                types = listOf(
                    TypeKeyNameUrlDto(
                        technicalKey = "HEADQUARTER",
                        name = "Headquarter",
                        url = ""
                    )
                )
            )
        ),
        profileClassifications = listOf(),
        types = listOf(
            TypeKeyNameUrlDto(
            technicalKey = "LEGAL_ENTITY",
            name = "Legal Entity",
            url = ""
          )
        ),
        bankAccounts = listOf(),
        roles = listOf(),
        relations = listOf()
    )
)
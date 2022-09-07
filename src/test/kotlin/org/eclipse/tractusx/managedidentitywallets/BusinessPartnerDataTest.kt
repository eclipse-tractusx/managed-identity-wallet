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

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import kotlin.test.*

@kotlinx.serialization.ExperimentalSerializationApi
class BusinessPartnerDataTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.stop(1000, 10000)
    }

    @Test
    fun testDataUpdate() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService =  EnvironmentTestSetup.revocationMockedService
        }) {
            handleRequest(HttpMethod.Post, "/api/businessPartnerDataRefresh") {
                addHeader(HttpHeaders.Authorization, "Bearer ${EnvironmentTestSetup.UPDATE_TOKEN}")
                addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.Accepted, response.status())
            }
        }
    }

    @Test
    fun testBusinessPartnerDataModel() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configurePersistence()
            configureOpenAPI()
            configureSecurity()
            configureRouting(EnvironmentTestSetup.walletService)
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,  EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.utilsService)
            configureSerialization()
        }) {
            val businessPartnerDataAsJson =
                """{
  "bpn": "BPNL000000000001",
  "identifiers": [
    {
      "uuid": "089e828d-01ed-4d3e-ab1e-cccca26814b3",
      "value": "BPNL000000000001",
      "type": {
        "technicalKey": "BPN",
        "name": "Business Partner Number",
        "url": ""
      },
      "issuingBody": {
        "technicalKey": "CATENAX",
        "name": "Catena-X",
        "url": ""
      },
      "status": {
        "technicalKey": "UNKNOWN",
        "name": "Unknown"
      }
    }
  ],
  "names": [
    {
      "uuid": "de3f3db6-e337-436b-a4e0-fc7d17e8af89",
      "value": "German Car Company",
      "shortName": "GCC",
      "type": {
        "technicalKey": "REGISTERED",
        "name": "The main name under which a business is officially registered in a country's business register.",
        "url": ""
      },
      "language": {
        "technicalKey": "undefined",
        "name": "Undefined"
      }
    },
    {
      "uuid": "defc3da4-92ef-44d9-9aee-dcedc2d72e0e",
      "value": "German Car Company",
      "shortName": "GCC",
      "type": {
        "technicalKey": "INTERNATIONAL",
        "name": "The international version of the local name of a business partner",
        "url": ""
      },
      "language": {
        "technicalKey": "undefined",
        "name": "Undefined"
      }
    }
  ],
  "legalForm": {
    "technicalKey": "DE_AG",
    "name": "Aktiengesellschaft",
    "url": "",
    "mainAbbreviation": "AG",
    "language": {
      "technicalKey": "de",
      "name": "German"
    },
    "categories": [
      {
        "name": "AG",
        "url": ""
      }
    ]
  },
  "status": null,
  "addresses": [
    {
      "uuid": "16701107-9559-4fdf-b1c1-8c98799d779d",
      "version": {
        "characterSet": {
          "technicalKey": "WESTERN_LATIN_STANDARD",
          "name": "Western Latin Standard (ISO 8859-1; Latin-1)"
        },
        "language": {
          "technicalKey": "en",
          "name": "English"
        }
      },
      "careOf": null,
      "contexts": [],
      "country": {
        "technicalKey": "DE",
        "name": "Germany"
      },
      "administrativeAreas": [
        {
          "uuid": "cc6de665-f8eb-45ed-b2bd-6caa28fa8368",
          "value": "Bavaria",
          "shortName": "BY",
          "fipsCode": "GM02",
          "type": {
            "technicalKey": "REGION",
            "name": "Region",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "postCodes": [
        {
          "uuid": "8a02b3d0-de1e-49a5-9528-cfde2d5273ed",
          "value": "80807",
          "type": {
            "technicalKey": "REGULAR",
            "name": "Regular",
            "url": ""
          }
        }
      ],
      "localities": [
        {
          "uuid": "2cd18685-fac9-49f4-a63b-322b28f7dc9a",
          "value": "Munich",
          "shortName": "M",
          "type": {
            "technicalKey": "CITY",
            "name": "City",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "thoroughfares": [
        {
          "uuid": "0c491424-b2bc-44cf-9d14-71cbe513423f",
          "value": "Muenchner Straße 34",
          "name": "Muenchner Straße",
          "shortName": null,
          "number": "34",
          "direction": null,
          "type": {
            "technicalKey": "STREET",
            "name": "Street",
            "url": ""
          },
          "language": {
            "technicalKey": "en",
            "name": "English"
          }
        }
      ],
      "premises": [],
      "postalDeliveryPoints": [],
      "geographicCoordinates": null,
      "types": [
        {
          "technicalKey": "HEADQUARTER",
          "name": "Headquarter",
          "url": ""
        }
      ]
    }
  ],
  "profileClassifications": [],
  "types": [
    {
      "technicalKey": "LEGAL_ENTITY",
      "name": "Legal Entity",
      "url": ""
    }
  ],
  "bankAccounts": [],
  "roles": [],
  "sites": [],
  "relations": [],
  "currentness": "2022-06-03T11:46:15.143429Z"
}""".trimIndent()
            val data: BusinessPartnerDataDto = Json.decodeFromString(businessPartnerDataAsJson)
            assertEquals(data.bpn,"BPNL000000000001")
            assertEquals(data.identifiers[0].issuingBody!!.name,"Catena-X")
            assertEquals(data.roles, emptyList())
        }
    }
}
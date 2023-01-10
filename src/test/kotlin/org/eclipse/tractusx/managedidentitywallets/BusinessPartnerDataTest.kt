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

import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.plugins.*
import org.eclipse.tractusx.managedidentitywallets.routes.appRoutes
import java.io.File
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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,
                EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.webhookService,
                EnvironmentTestSetup.utilsService)
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
            appRoutes(EnvironmentTestSetup.walletService, EnvironmentTestSetup.bpdService,
                EnvironmentTestSetup.revocationMockedService, EnvironmentTestSetup.webhookService,
                EnvironmentTestSetup.utilsService)
            configureSerialization()
        }) {
            val businessPartnerDataAsJson: String = File("./src/test/resources/bpdm-test-data/legalEntity.json")
                .readText(Charsets.UTF_8)
            val data: List<BusinessPartnerDataDto> = Json.decodeFromString(businessPartnerDataAsJson)
            assertEquals("BPNL000000000001", data[0].bpn)
            assertEquals(emptyList(), data[0].roles)
            assertEquals(null, data[0].status)

            val legaAddressAsString: String = File("./src/test/resources/bpdm-test-data/legalAddressOfEntity.json")
                .readText(Charsets.UTF_8)

            val address: List<LegalAddressDto> = Json.decodeFromString(legaAddressAsString)
            assertEquals(emptyList(), address[0].legalAddress.premises)
            assertEquals("WESTERN_LATIN_STANDARD", address[0].legalAddress.version.characterSet.technicalKey)
            assertEquals(1, address[0].legalAddress.administrativeAreas.size)
            assertEquals("Münchner Straße 34", address[0].legalAddress.thoroughfares[0].value)
        }
    }
}

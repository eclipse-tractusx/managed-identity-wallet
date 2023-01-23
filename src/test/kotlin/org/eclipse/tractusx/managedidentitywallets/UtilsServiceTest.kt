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

import io.ktor.server.testing.*
import org.eclipse.tractusx.managedidentitywallets.models.UnprocessableEntityException
import org.eclipse.tractusx.managedidentitywallets.plugins.configureSerialization
import org.eclipse.tractusx.managedidentitywallets.plugins.configureStatusPages
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@kotlinx.serialization.ExperimentalSerializationApi
class UtilsServiceTest {

    private val server = TestServer().initServer()

    @BeforeTest
    fun setup() {
        server.start()
    }

    @AfterTest
    fun tearDown() {
        SingletonTestData.cleanSingletonTestData()
        server.stop(1000, 10000)
    }

    @Test
    fun testDidDocumentOperations() {
        withTestApplication({
            EnvironmentTestSetup.setupEnvironment(environment)
            configureSerialization()
            configureStatusPages()
            Services.walletService = EnvironmentTestSetup.walletService
            Services.businessPartnerDataService = EnvironmentTestSetup.bpdService
            Services.utilsService = EnvironmentTestSetup.utilsService
            Services.revocationService = EnvironmentTestSetup.revocationMockedService
            Services.webhookService = EnvironmentTestSetup.webhookService
        }) {

            assertDoesNotThrow {
                EnvironmentTestSetup.utilsService.checkIndyDid("did:sov:ArqouCjqi4RwBXQqjAbQrG")
            }

            assertThrows<UnprocessableEntityException> {
                EnvironmentTestSetup.utilsService.checkIndyDid("did:sov:ArqouCjqi4RwBXQqjAbQrG xwrong")
            }

            assertThrows<UnprocessableEntityException> {
                EnvironmentTestSetup.utilsService.checkIndyDid("did:sov:shortdid")
            }

            assertThrows<UnprocessableEntityException> {
                EnvironmentTestSetup.utilsService.checkIndyDid("did:sov: ArqouCjqi4RwBXQqjAbQrG")
            }

            assertThrows<UnprocessableEntityException> {
                EnvironmentTestSetup.utilsService.checkIndyDid("did:www:ArqouCjqi4RwBXQqjAbQrG")
            }
        }
    }

}

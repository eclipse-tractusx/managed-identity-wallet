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

package org.eclipse.tractusx.managedidentitywallets.plugins

import io.bkbn.kompendium.core.Kompendium
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.info.Contact
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.info.License
import io.ktor.application.*
import java.net.URI

fun Application.configureOpenAPI() {
    val version = environment.config.property("app.version").getString()
    val title = environment.config.property("openapi.title").getString()
    val description = environment.config.property("openapi.description").getString()
    val termsOfServiceUrl = environment.config.property("openapi.termsOfServiceUrl").getString()
    val contactName = environment.config.property("openapi.contactName").getString()
    val contactEmail = environment.config.property("openapi.contactEmail").getString()
    val contactUrl = environment.config.property("openapi.contactUrl").getString()
    val licenseName = environment.config.property("openapi.licenseName").getString()
    val licenseUrl = environment.config.property("openapi.licenseUrl").getString()

    install(Kompendium) {
      spec = OpenApiSpec(
          openapi = "3.0.3",
          info = Info(
              title = title,
              version = version,
              description = description,
              termsOfService = URI(termsOfServiceUrl),
              contact = Contact(
                  name = contactName,
                  email = contactEmail,
                  url = URI(contactUrl)
              ),
              license = License(
                  name = licenseName,
                  url = URI(licenseUrl)
              )
          )
      )
    }
}

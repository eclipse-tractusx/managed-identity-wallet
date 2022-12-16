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

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import org.eclipse.tractusx.managedidentitywallets.models.*
import org.eclipse.tractusx.managedidentitywallets.models.BadRequestException
import org.eclipse.tractusx.managedidentitywallets.models.NotFoundException

fun Application.configureStatusPages() {
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
        exception<AuthorizationException> { cause ->
            call.respond(HttpStatusCode.Unauthorized, ExceptionResponse(cause.message!!))
        }
        exception<InternalServerErrorException> { cause ->
            call.respond(HttpStatusCode.InternalServerError, ExceptionResponse(cause.message!!))
        }
    }
}

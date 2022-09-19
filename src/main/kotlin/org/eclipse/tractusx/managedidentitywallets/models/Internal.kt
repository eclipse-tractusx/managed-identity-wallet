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

package org.eclipse.tractusx.managedidentitywallets.models

import io.bkbn.kompendium.core.metadata.ExceptionInfo
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class ExceptionResponse(val message: String, val error: Boolean = true)

@Serializable
data class SuccessResponse(val message: String)

class BadRequestException(message: String? = "empty message") : Exception(message)

class NotImplementedException(message: String? = "empty message") : Exception(message)

class UnprocessableEntityException(message: String? = "empty message") : Exception(message)

class ConflictException(message: String? =  "empty message") : Exception(message)

class NotFoundException(message: String? =  "empty message") : Exception(message)

class ForbiddenException(message: String? = "empty message") : Exception(message)

class AuthorizationException(message: String? = "empty message") : Exception(message)

class InternalServerErrorException(message: String? = "empty message") : Exception(message)

val semanticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input can not be processed due to semantic mismatches",
    status = HttpStatusCode.UnprocessableEntity,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val syntacticallyInvalidInputException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The input does not comply to the syntax requirements",
    status = HttpStatusCode.BadRequest,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val notFoundException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The required entity does not exists",
    status = HttpStatusCode.NotFound,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val conflictException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The request could not be completed due to a conflict.",
    status = HttpStatusCode.Conflict,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val unauthorizedException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The request could not be completed due to a failed authorization.",
    status = HttpStatusCode.Unauthorized,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

val forbiddenException = ExceptionInfo<ExceptionResponse>(
    responseType = typeOf<ExceptionResponse>(),
    description = "The request could not be completed due to a forbidden access.",
    status = HttpStatusCode.Forbidden,
    examples = mapOf("demo" to ExceptionResponse("reason"))
)

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

package net.catenax.core.managedidentitywallets.plugins

import io.ktor.serialization.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.builtins.*

import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.ktor.client.features.json.*
import kotlinx.serialization.json.JsonNull.isString

import java.time.LocalDateTime
import java.util.Date

import net.catenax.core.managedidentitywallets.models.*
import java.net.URI
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

object StringListSerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

object URISerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): URI = URI.create(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
}

// https://github.com/Kotlin/kotlinx.serialization/issues/746#issuecomment-779549456
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")
    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonElement = serializeAny(value)
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    private fun serializeAny(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Map<*, *> -> {
            val mapContents = value.entries.associate { mapEntry ->
                mapEntry.key.toString() to serializeAny(mapEntry.value)
            }
            JsonObject(mapContents)
        }
        is List<*> -> {
            val arrayContents = value.map { listEntry -> serializeAny(listEntry) }
            JsonArray(arrayContents)
        }
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is String ->  JsonPrimitive(value)
        else -> {
            val contents = value::class.memberProperties.associate { property ->
                property.name to serializeAny(property.getter.call(value))
            }
            JsonObject(contents)
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()

        return deserializeJsonElement(element)
    }

    private fun deserializeJsonElement(element: JsonElement): Any = when (element) {
        is JsonObject -> {
            element.mapValues { deserializeJsonElement(it.value) }
        }
        is JsonArray -> {
            element.map { deserializeJsonElement(it) }
        }
        is JsonPrimitive  -> {
            if (element.isString) {
               element.content
           } else {
               try {
                    element.boolean
                } catch (e: Throwable) {
                    try {
                        element.long
                    } catch (e: Throwable) {
                        element.double
                    }
                }
            }
        }
    }
}

@kotlinx.serialization.ExperimentalSerializationApi
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = false
            encodeDefaults = true

            // useArrayPolymorphism  = true
            classDiscriminator = "class"
            serializersModule = KompendiumSerializersModule.module
            explicitNulls = false

            // currently needed for business partner data update
            ignoreUnknownKeys = true
        })
    }
}

package net.catenax.core.custodian.plugins

// for 2.0.0-beta
// import io.ktor.serialization.kotlinx.json.*
// import io.ktor.server.plugins.*
// import io.ktor.server.application.*
// import io.ktor.server.response.*
// import io.ktor.server.request.*
// import io.ktor.server.routing.*

// for 1.6.7
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

import java.time.LocalDateTime
import java.util.Date

import net.catenax.core.custodian.models.*

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

object CompanyCreateListSerializer : JsonTransformingSerializer<List<CompanyCreateDto>>(ListSerializer(CompanyCreateDto.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            
            // useArrayPolymorphism  = true
            classDiscriminator = "class"
            serializersModule = KompendiumSerializersModule.module
            explicitNulls = false
        })
    }
}

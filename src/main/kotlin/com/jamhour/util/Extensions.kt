package com.jamhour.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.net.http.HttpResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun String.toURI() = URI.create(this)

inline fun <reified T> StringFormat.toBodyHandler() = HttpResponse.BodyHandler {
    HttpResponse.BodySubscribers.mapping(
        HttpResponse.BodySubscribers.ofString(Charsets.UTF_8)
    ) {
        runCatching { decodeFromString<T>(it) }.getOrNull()
    }
}

object URISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor(URI::javaClass.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = decoder.decodeString().toURI()
}

class ZonedDateTimeSerializer(
    val format: DateTimeFormatter
) : KSerializer<ZonedDateTime> {
    override val descriptor = PrimitiveSerialDescriptor(ZonedDateTime::javaClass.name, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ZonedDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): ZonedDateTime = ZonedDateTime.parse(decoder.decodeString(), format)
}


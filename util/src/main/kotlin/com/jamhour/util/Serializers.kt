package com.jamhour.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

class LocalDateSerializer(
    private val format: DateTimeFormatter
) : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(LocalDate::javaClass.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), format)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(format))
}
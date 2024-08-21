package com.jamhour.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Logger

fun String.toURI() = URI.create(this)

inline fun <reified T> StringFormat.toBodyHandler(logger: Logger? = null) = HttpResponse.BodyHandler {
    HttpResponse.BodySubscribers.mapping(
        HttpResponse.BodySubscribers.ofString(Charsets.UTF_8)
    ) {
        logger?.info { "Request response received" }
        try {
            logger?.info { "Converting response" }
            return@mapping decodeFromString<T>(it).also { logger?.info { "Successfully converted response" } }
        } catch (e: Exception) {
            logger?.severe { "An error occurred: ${e.stackTraceToString()}" }
            return@mapping null.also { logger?.info { "Returning null" } }
        }
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

class LocalDateSerializer(
    private val format: DateTimeFormatter
) : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(LocalDate::javaClass.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), format)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(format))
}

fun loggerFactory(
    loggerForClass: Class<*>
): Logger {

    val logger = Logger.getLogger(loggerForClass.name).apply {
        useParentHandlers = false
    }

    val consoleHandler = ConsoleHandler().apply {
        formatter = object : Formatter() {
            override fun format(record: java.util.logging.LogRecord): String {
                val toLocalDateTime = record.instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                return buildString {
                    append("[").append(loggerForClass.name).append("]").append(" ")
                    append("[").append(record.sourceMethodName).append("]").append(" ")
                    append("[").append(Thread.currentThread().name).append("]").append(" ")
                    append("[").append(toLocalDateTime).append("]").append(": ")
                    append("[").append(record.level.name).append("] - ").append(record.message)
                    appendLine()
                }
            }
        }
    }

    return logger.apply { addHandler(consoleHandler) }
}



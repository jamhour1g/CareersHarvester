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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

fun String.toURI() = URI.create(this)

inline fun <reified T> StringFormat.toBodyHandler(logger: Logger? = null) = HttpResponse.BodyHandler {
    HttpResponse.BodySubscribers.mapping(
        HttpResponse.BodySubscribers.ofString(Charsets.UTF_8)
    ) {
        logger?.info { "Received response from request" }
        try {
            logger?.info { "Attempting to convert response to ${T::class.simpleName}" }
            val result = decodeFromString<T>(it)
            logger?.info { "Successfully converted response to ${T::class.simpleName}" }
            result
        } catch (e: Exception) {
            logger?.severe { "Error converting response to ${T::class.simpleName}: ${e.stackTraceToString()}" }
            null.also { logger?.info { "Returning null due to conversion error" } }
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

fun loggerFactory(loggerForClass: Class<*>): Logger {
    val logger = Logger.getLogger(loggerForClass.name).apply {
        useParentHandlers = false
    }

    val consoleHandler = ConsoleHandler().apply {
        formatter = LogFormatter(loggerForClass.name)
    }

    return logger.apply { addHandler(consoleHandler) }
}

private class LogFormatter(private val className: String) : Formatter() {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss")

    override fun format(record: LogRecord): String {
        val toLocalDateTime = record.instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return buildString {
            append("[$className] ")
            append("[${record.sourceMethodName}] ")
            append("[${Thread.currentThread().name}] ")
            append("[${toLocalDateTime.format(dateTimeFormatter)}] ")
            append("[${record.level.name}] - ")
            append(record.message)
            appendLine()
        }
    }
}



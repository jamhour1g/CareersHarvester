package com.jamhour.util

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.io.path.Path

private val isConsoleLoggingEnabled by lazy { System.getenv("ENABLE_CONSOLE_LOGGING")?.lowercase() == "true" }
private val isFileLoggingEnabled by lazy { System.getenv("ENABLE_FILE_LOGGING")?.lowercase() == "true" }

fun loggerFactory(loggerForClass: Class<*>): Logger {
    return Logger.getLogger(loggerForClass.name).apply {
        useParentHandlers = false
        addConsoleHandler()
        addFileHandler(loggerForClass)
    }
}

private fun Logger.addConsoleHandler() = apply {
    if (isConsoleLoggingEnabled) {
        addHandler(ConsoleHandler().apply {
            formatter = LogFormatter()
        })
    }
}

private fun Logger.addFileHandler(loggerForClass: Class<*>): Logger {
    val timeFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm"))
    val fileFormat = "logs/${loggerForClass.packageName}/${loggerForClass.simpleName}"
    val filePath = Path(fileFormat)

    createLogDirectory(filePath)

    if (isFileLoggingEnabled) {
        addHandler(
            FileHandler("$fileFormat/$timeFormat.log", true)
                .apply {
                    formatter = LogFormatter()
                }
        )
    }

    return this
}

private fun Logger.createLogDirectory(filePath: Path) {
    if (Files.notExists(filePath)) {
        runCatching { Files.createDirectories(filePath) }
            .onFailure {
                Logger.getLogger("LoggerSetup").severe { "Error creating directory: ${filePath.toAbsolutePath()}" }
            }
    }
}

private class LogFormatter() : Formatter() {
    override fun format(record: LogRecord) = buildString {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss")

        append("[${record.loggerName}] ")
        append("[${record.sourceMethodName}] ")
        append("[${Thread.currentThread().name}] ")
        append("[${LocalDateTime.now().format(dateTimeFormatter)}] ")
        append("[${record.level.name}] - ${record.message}")
        record.thrown?.let {
            appendLine()
            appendLine("Exception: ${it.message}")
            appendLine("Cause: ${it.cause}")
            appendLine("Stacktrace: ${it.stackTraceToString()}")
        }
        appendLine()
    }
}
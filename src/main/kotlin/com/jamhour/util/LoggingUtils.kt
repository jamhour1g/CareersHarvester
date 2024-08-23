package com.jamhour.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

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
package com.simplestsoft.twostrokecalc.domain.vehicles

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val DATE_TIME_FORMATTERS = listOf(
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY),
    DateTimeFormatter.ofPattern("d.M.yyyy H:mm", Locale.GERMANY),
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.GERMANY),
)

private val DATE_ONLY_FORMATTERS = listOf(
    DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY),
    DateTimeFormatter.ofPattern("d.M.uuuu", Locale.GERMANY),
    DateTimeFormatter.ISO_LOCAL_DATE,
)

fun parseVehicleDateTime(raw: String): LocalDateTime? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    DATE_TIME_FORMATTERS.forEach { formatter ->
        try {
            return LocalDateTime.parse(trimmed, formatter)
        } catch (_: DateTimeParseException) {
        }
    }

    DATE_ONLY_FORMATTERS.forEach { formatter ->
        try {
            return LocalDate.parse(trimmed, formatter).atStartOfDay()
        } catch (_: DateTimeParseException) {
        }
    }

    return null
}

fun parseVehicleDate(raw: String): LocalDate? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    DATE_ONLY_FORMATTERS.forEach { formatter ->
        try {
            return LocalDate.parse(trimmed, formatter)
        } catch (_: DateTimeParseException) {
        }
    }

    return parseVehicleDateTime(trimmed)?.toLocalDate()
}

fun formatVehicleDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMANY))

fun formatVehicleDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY))

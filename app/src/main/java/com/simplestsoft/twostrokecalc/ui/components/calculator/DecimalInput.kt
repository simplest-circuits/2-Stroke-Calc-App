package com.simplestsoft.twostrokecalc.ui.components.calculator

fun parseDecimal(text: String): Double? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    return normalized.toDoubleOrNull()
}

fun parsePositiveDecimal(text: String): Double? =
    parseDecimal(text)?.takeIf { it > 0.0 }

fun String.filterDecimalInput(): String {
    val builder = StringBuilder()
    var separatorSeen = false
    for (char in this) {
        when {
            char.isDigit() -> builder.append(char)
            (char == '.' || char == ',') && !separatorSeen -> {
                builder.append(char)
                separatorSeen = true
            }
        }
    }
    return builder.toString()
}

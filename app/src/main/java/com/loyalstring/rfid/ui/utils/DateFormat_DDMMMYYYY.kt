package com.loyalstring.rfid.ui.utils

fun formatDate_ddMMyyyy(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "-"

    val outputFormat = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")

    val inputFormats = listOf(
        java.time.format.DateTimeFormatter.ISO_DATE_TIME,
        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    )

    for (formatter in inputFormats) {
        try {
            val dateTime = java.time.LocalDateTime.parse(dateStr, formatter)
            return dateTime.format(outputFormat)
        } catch (_: Exception) {
            // try next format
        }
    }

    return "-"
}

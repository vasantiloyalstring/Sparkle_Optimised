package com.loyalstring.rfid.ui.utils

fun formatDate_ddMMyyyy(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "-"

    return try {
        val inputFormat = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val outputFormat = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")

        val dateTime = java.time.LocalDateTime.parse(dateStr, inputFormat)
        dateTime.format(outputFormat)
    } catch (e: Exception) {
        "-"
    }
}

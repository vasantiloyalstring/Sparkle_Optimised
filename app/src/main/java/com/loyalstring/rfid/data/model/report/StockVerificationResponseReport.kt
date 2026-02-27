package com.loyalstring.rfid.data.model.report

data class StockVerificationResponseReport(
    val Message: String?,
    val ReportDate: String?,
    val TotalRecordsFetched: Int?,
    val Branches: List<Branch>?,
    val Totals: Totals?
)

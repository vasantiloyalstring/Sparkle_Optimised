package com.loyalstring.rfid.data.model.report

data class BatchTotals(
    val TotalQty: Int?,
    val TotalGrossWeight: Double?,
    val TotalNetWeight: Double?,
    val TotalMatchQty: Int?,
    val TotalUnmatchQty: Int?,
    val TotalMatchGrossWeight: Double?,
    val TotalMatchNetWeight: Double?,
    val TotalUnmatchGrossWeight: Double?,
    val TotalUnmatchNetWeight: Double?
)

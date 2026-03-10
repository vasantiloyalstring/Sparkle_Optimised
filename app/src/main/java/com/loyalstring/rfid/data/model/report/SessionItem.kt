package com.loyalstring.rfid.data.model.report

data class SessionItem(    val SessionNumber: Int,
                           val SessionId: String,
                           val ScanBatchId: String,
                           val BatchName: String,
                           val BranchId: Int?,
                           val BranchName: String?,
                           val StartedOn: String,
                           val EndedOn: String,
                           val TotalQty: Int,
                           val MatchQty: Int,
                           val UnmatchQty: Int
)

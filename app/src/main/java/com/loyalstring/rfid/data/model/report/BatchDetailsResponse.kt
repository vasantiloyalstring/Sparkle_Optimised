package com.loyalstring.rfid.data.model.report

data class BatchDetailsResponse(val Message: String?,
                                 val ClientCode: String?,
                                 val ScanBatchId: String?,
                                 val SessionId: String?,
                                 val SessionNumber: Int?,
                                 val BatchName: String?,
                                 val BranchId: Int?,
                                 val BranchName: String?,
                                 val TotalSessions: Int?,
                                 val MatchedList: List<BatchItem>?,
                                 val UnmatchedList: List<BatchItem>?,
                                 val Totals: BatchTotals?)

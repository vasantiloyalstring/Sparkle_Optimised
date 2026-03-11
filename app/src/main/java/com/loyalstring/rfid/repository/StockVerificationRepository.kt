package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.report.BatchDetailsResponse
import com.loyalstring.rfid.data.model.report.ScanBatchRequest
import com.loyalstring.rfid.data.model.report.SessionListResponse
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import retrofit2.Response
import retrofit2.http.Body

interface StockVerificationRepository {
    suspend fun getStockVerificationReport(
        request: StockVerificationReqReport
    ): StockVerificationResponseReport

    suspend fun getSessionList(
        request: ClientCodeRequest
    ): SessionListResponse

    suspend fun getBatchDetails(
        request: ScanBatchRequest
    ): BatchDetailsResponse

   /* suspend fun getFilteredSessions(
        clientCode: String,
        branchId: Int?,
        fromDate: String,
        toDate: String
    ): SessionListResponse*/
}
package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport

interface StockVerificationRepository {
    suspend fun getStockVerificationReport(
        request: StockVerificationReqReport
    ): StockVerificationResponseReport
}
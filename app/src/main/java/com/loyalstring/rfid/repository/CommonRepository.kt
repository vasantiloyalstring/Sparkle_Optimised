package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData
import okhttp3.RequestBody

interface CommonRepository {

    suspend fun stockVarificationNew(
        request: StockVerificationRequestData
    ): Result<ScanSessionResponse>

    suspend fun uploadStock(
        request: RequestBody
    ): Result<ScanSessionResponse>
}
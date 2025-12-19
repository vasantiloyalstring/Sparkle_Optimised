package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData

interface CommonRepository {

    suspend fun stockVarificationNew(
        request: StockVerificationRequestData
    ): Result<ScanSessionResponse>
}
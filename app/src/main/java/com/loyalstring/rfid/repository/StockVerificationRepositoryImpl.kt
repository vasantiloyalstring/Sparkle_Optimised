package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockVerificationRepositoryImpl @Inject constructor(
    private val api: RetrofitInterface
) : StockVerificationRepository {

    override suspend fun getStockVerificationReport(
        request: StockVerificationReqReport
    ): StockVerificationResponseReport {

        val response = api.stockVerificationReport(request)

        if (response.isSuccessful) {
            return response.body()
                ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.message())
        }
    }
}
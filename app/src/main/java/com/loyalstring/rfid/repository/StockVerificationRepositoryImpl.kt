package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.report.BatchDetailsResponse
import com.loyalstring.rfid.data.model.report.ScanBatchRequest
import com.loyalstring.rfid.data.model.report.SessionListResponse
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import retrofit2.Response
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

    override suspend fun getSessionList(
        request: ClientCodeRequest
    ): SessionListResponse {

        val response = api.stockVerificationBatchwise(request)

        if (response.isSuccessful) {

            return response.body()
                ?: throw Exception("Empty response")

        } else {

            throw Exception(response.message())

        }
    }

    override suspend fun getBatchDetails(
        request: ScanBatchRequest
    ): BatchDetailsResponse {

        val response = api.stockVerificationBatchwiseItem(request)

        if (response.isSuccessful) {

            return response.body()
                ?: throw Exception("Empty response")

        } else {

            throw Exception(response.message())

        }
    }

  /*  override suspend fun getFilteredSessions(
        clientCode: String,
        branchId: Int?,
        fromDate: String,
        toDate: String
    ): SessionListResponse {

        val response = api.getFilteredSessions(
            FilterSessionRequest(
                ClientCode = clientCode,
                BranchId = branchId,
                FromDate = fromDate,
                ToDate = toDate
            )
        )

        if (response.isSuccessful) {

            return response.body()
                ?: throw Exception("Empty response")

        } else {

            throw Exception(response.message())
        }
    }*/
}
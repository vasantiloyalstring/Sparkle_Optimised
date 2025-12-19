package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData
import com.loyalstring.rfid.data.remote.api.RetrofitInterface

class CommonRepoImple ( private val api: RetrofitInterface
) : CommonRepository {

    override suspend fun stockVarificationNew(
        request: StockVerificationRequestData
    ): Result<ScanSessionResponse> {
        return try {
            val response = api.stockVarificationNew(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errMsg = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "API error: ${response.code()} - ${response.message()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
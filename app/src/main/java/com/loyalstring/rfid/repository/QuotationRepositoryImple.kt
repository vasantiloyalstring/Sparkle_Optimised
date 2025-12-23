package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import com.loyalstring.rfid.data.model.quotation.AddQuotationRequest
import com.loyalstring.rfid.data.model.quotation.AddQuotationResponse
import com.loyalstring.rfid.data.model.quotation.LastQuotationNoResponse
import com.loyalstring.rfid.data.model.quotation.QuotationListRequest
import com.loyalstring.rfid.data.model.quotation.QuotationListResponse
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationRequest
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import javax.inject.Inject

class QuotationRepositoryImple @Inject constructor(
    private val api: RetrofitInterface

) : QuotationRepository {

    override suspend fun getAllQuotationList(
        request: QuotationListRequest
    ): Result<List<QuotationListResponse>> {
        return try {
            val res = api.getAllQuotation(request)
            if (res.isSuccessful) {
                Result.success(res.body().orEmpty())
            } else {
                // try to read backend error message (if any)
                val msg = res.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "API failed with code ${res.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun saveQuotation(
        request: AddQuotationRequest
    ): Result<QuotationListResponse> {
        return try {
            val response = api.saveQuotation(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(
                    Exception("API error: ${response.code()} - ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastQuotationNo(
        request: ClientCodeRequest
    ): Result<LastQuotationNoResponse> {
        return try {
            val res = api.getLastQuotationNo(request)

            if (res.isSuccessful) {
                val body = res.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Empty response body"))
            } else {
                val msg = res.errorBody()?.string().orEmpty().ifBlank {
                    "API error: ${res.code()} - ${res.message()}"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuotation(
        request: UpdateQuotationRequest
    ): Result<UpdateQuotationResponse> {
        return try {
            val res = api.updateQuotation(request)

            if (res.isSuccessful) {
                val body = res.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val msg = res.errorBody()?.string().orEmpty().ifBlank {
                    "API error: ${res.code()} - ${res.message()}"
                }
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



}
package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutLastNoReq
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import retrofit2.Response

class SampleOutRepositoryImpl (
    private val api: RetrofitInterface
    ) : SampleOutRepositoty {

        override suspend fun getAllSampleOut(
            request: SampleOutListRequest
        ): Result<List<SampleOutListResponse>> {
            return try {
                val response = api.getAllSampleOut(request)

                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    Result.success(body)
                } else {
                    Result.failure(
                        Exception(
                            "API error: ${response.code()} - ${response.message()}"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun addSampleOut(
        request: SampleOutAddRequest
    ): Response<SampleOutAddResponse> {
        return api.addSampleOut(request)
    }

    override suspend fun lastSampleOutNo(
        request: SampleOutLastNoReq
    ): Result<String> {
        return try {
            // yaha Retrofit se direct String aa raha hai, Response<...> nahi
            val sampleOutNo = api.lastSampleOutNo(request) // e.g. "C14"
            Result.success(sampleOutNo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSampleOut(
        request: SampleOutUpdateRequest
    ): Response<SampleOutAddResponse> {
        return api.updateSampleOut(request)
    }
}
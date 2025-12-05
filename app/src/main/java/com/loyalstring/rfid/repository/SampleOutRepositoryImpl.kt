package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface

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
}
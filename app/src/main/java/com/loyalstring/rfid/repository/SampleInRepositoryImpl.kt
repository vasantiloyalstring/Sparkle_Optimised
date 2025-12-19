package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface

class SampleInRepositoryImpl (
    private val api: RetrofitInterface
) : SampleInRepository {

    override suspend fun getAllSampleIn(
        request: SampleOutListRequest
    ): Result<List<SampleInResponse>> {
        return try {
            val response = api.getAllSampleIn(request)

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
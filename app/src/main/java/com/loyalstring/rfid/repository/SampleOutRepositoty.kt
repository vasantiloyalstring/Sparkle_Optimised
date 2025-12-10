package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutLastNoReq
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import retrofit2.Response

interface SampleOutRepositoty {
    suspend fun getAllSampleOut(
        request: SampleOutListRequest
    ): Result<List<SampleOutListResponse>>

    suspend fun addSampleOut(
        request: SampleOutAddRequest
    ): Response<SampleOutAddResponse>

    suspend fun lastSampleOutNo(
        request: SampleOutLastNoReq
    ): Result<String>

    suspend fun updateSampleOut(
        request: SampleOutUpdateRequest
    ): Response<SampleOutAddResponse>
}
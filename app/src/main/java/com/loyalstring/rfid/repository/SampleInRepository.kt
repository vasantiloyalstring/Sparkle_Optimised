package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse

interface SampleInRepository {

    suspend fun getAllSampleIn(
        request: SampleOutListRequest
    ): Result<List<SampleInResponse>>
}
package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse

interface SampleOutRepositoty {
    suspend fun getAllSampleOut(
        request: SampleOutListRequest
    ): Result<List<SampleOutListResponse>>
}
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
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse

interface QuotationRepository {
    suspend fun getAllQuotationList(
        request: QuotationListRequest
    ): Result<List<QuotationListResponse>>

    suspend fun saveQuotation(
        request: AddQuotationRequest
    ): Result<QuotationListResponse>

    suspend fun getLastQuotationNo(
        request: ClientCodeRequest
    ): Result<LastQuotationNoResponse>

    suspend fun updateQuotation(
        request: UpdateQuotationRequest
    ): Result<UpdateQuotationResponse>
}
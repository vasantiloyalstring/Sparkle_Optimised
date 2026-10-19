package com.loyalstring.rfid.repository


import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoResponse
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import retrofit2.Response
import okhttp3.ResponseBody

interface DeliveryChallanRepository {
    suspend fun getAllDeliveryChallans(request: DeliveryChallanRequestList): Response<List<DeliveryChallanResponseList>>

    /** Streaming body — parse list rows without nested ChallanDetails allocation. */
    suspend fun getAllDeliveryChallansRaw(request: DeliveryChallanRequestList): Response<ResponseBody>

    suspend fun getLastChallanNo(request: ChallanNoRequest): Response<ChallanNoResponse>
    suspend fun addDeliveryChallan(request: AddDeliveryChallanRequest): Response<AddDeliveryChallanResponse>
    suspend fun updateDeliveryChallan(request: UpdateDeliveryChallanRequest): Response<AddDeliveryChallanResponse>
    suspend fun getAllCustomerTunch(request: CustomerTunchRequest): Response<List<CustomerTunchResponse>>
    suspend fun deleteDeliveryChallan(request: DeleteDeliveryChallanRequest): Response<DeleteDeliveryChallanResponse>

   // suspend fun getDeliveryChallanById(clientCode: String, challanId: Int): Response<DeliveryChallanResponseList>

}
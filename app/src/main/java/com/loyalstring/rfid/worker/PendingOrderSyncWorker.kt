package com.loyalstring.rfid.worker
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.dao.PendingOrderDao
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class PendingOrderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingOrderDao,
    private val api: RetrofitInterface
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending = dao.getAllPending()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        pending.forEach { row ->
            try {
                val req = Gson().fromJson(row.payloadJson, CustomOrderRequest::class.java)

                // ⚠️ NOTE: yaha actual response parsing karo (example)
                val nextNoResp = api.getLastOrderNo(ClientCodeRequest(req.ClientCode))
                val serverOrderNo = nextNoResp.body()?.LastOrderNo?.toString() ?: return Result.retry()
                Log.d("serverOrderNo","serverOrderNo"+serverOrderNo)
                val newOrderNoStr = (( nextNoResp.body()?.LastOrderNo?.toIntOrNull() ?: return Result.retry()) + 1).toString()

                Log.d("serverOrderNo", "serverOrderNo=$ nextNoResp.body()?.LastOrderNo?  newOrderNo=$newOrderNoStr")

                val newReq = req.copy(OrderNo = newOrderNoStr)

                val savedResp = api.addOrder(newReq)
                if (!savedResp.isSuccessful) throw Exception("Save failed: ${savedResp.code()}")

                dao.updateStatus(
                    localId = row.localId,
                    status = "SYNCED",
                    err = null,
                    attempts = row.attempts,
                    serverNo = newOrderNoStr,
                    serverId = savedResp.body()?.CustomOrderId
                )
            } catch (e: Exception) {
                anyFailed = true
                dao.updateStatus(
                    localId = row.localId,
                    status = "PENDING",
                    err = e.message,
                    attempts = row.attempts + 1,
                    serverNo = row.serverOrderNo,
                    serverId = row.serverOrderId
                )
            }
        }

        // agar koi fail hua to retry so WM firse chalaye
        return if (anyFailed) Result.retry() else Result.success()
    }
}

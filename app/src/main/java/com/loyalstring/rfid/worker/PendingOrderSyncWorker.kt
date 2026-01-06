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
import com.loyalstring.rfid.data.remote.data.DeleteOrderRequest

@HiltWorker
class PendingOrderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingOrderDao,
    private val api: RetrofitInterface
) : CoroutineWorker(appContext, params) {

 /*   override suspend fun doWork(): Result {
        val pending = dao.getAllPending()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false


        // ✅ 1) DELETE first
        val deletes = pending.filter { it.op.equals("DELETE", true) }
        deletes.forEach { row ->
            try {
                val serverId = row.serverOrderId

                if (serverId == null || serverId == 0) {
                    // offline-created row deleted before sync -> just remove locally
                    dao.hardDeleteByLocalId(row.localId)
                } else {
                    // ✅ call delete API (adjust signature if needed)

                    val resp = api.deleteCustomerOrder(DeleteOrderRequest(row.clientCode,serverId))
                    if (!resp.isSuccessful) throw Exception("Delete failed: ${resp.code()}")
                    dao.hardDeleteByLocalId(row.localId)
                }
            } catch (e: Exception) {
                anyFailed = true
                dao.updateStatus(
                    localId = row.localId,
                    status = "PENDING",
                    err = e.message ?: "Delete error",
                    attempts = row.attempts + 1,
                    serverNo = row.serverOrderNo,
                    serverId = row.serverOrderId
                )
            }
        }
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
    }*/

    override suspend fun doWork(): Result {
        val pending = dao.getAllPending()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        val deletes = pending.filter { it.op.equals("DELETE", true) }
        val updates = pending.filter { it.op.equals("UPDATE", true) }
        val creates = pending.filter { it.op.equals("CREATE", true) || it.op.isBlank() }

        // 1) DELETE first
        deletes.forEach { row ->
            try {
                val serverId = row.serverOrderId
                if (serverId == null || serverId == 0) {
                    // offline-created + deleted before sync => remove locally only
                    dao.hardDeleteByLocalId(row.localId)
                } else {
                    val resp = api.deleteCustomerOrder(DeleteOrderRequest(row.clientCode, serverId))
                    if (!resp.isSuccessful) throw Exception("Delete failed: ${resp.code()}")
                    dao.hardDeleteByLocalId(row.localId)
                }
            } catch (e: Exception) {
                anyFailed = true
                dao.updateStatus(
                    localId = row.localId,
                    status = "PENDING",
                    err = e.message ?: "Delete error",
                    attempts = row.attempts + 1,
                    serverNo = row.serverOrderNo,
                    serverId = row.serverOrderId
                )
            }
        }

        // 2) UPDATE second (server id required)
        updates.forEach { row ->
            try {
                val serverId = row.serverOrderId
                if (serverId == null || serverId == 0) {
                    // no server id => treat as CREATE or keep pending
                    throw Exception("UPDATE missing serverOrderId")
                }

                val req = Gson().fromJson(row.payloadJson, CustomOrderRequest::class.java)
                val resp = api.updateCustomerOrder(req) // your update endpoint
                if (!resp.isSuccessful) throw Exception("Update failed: ${resp.code()}")

                dao.updateStatus(
                    localId = row.localId,
                    status = "SYNCED",
                    err = null,
                    attempts = row.attempts,
                    serverNo = row.serverOrderNo,
                    serverId = serverId
                )
            } catch (e: Exception) {
                anyFailed = true
                dao.updateStatus(
                    localId = row.localId,
                    status = "PENDING",
                    err = e.message ?: "Update error",
                    attempts = row.attempts + 1,
                    serverNo = row.serverOrderNo,
                    serverId = row.serverOrderId
                )
            }
        }

        // 3) CREATE last (generate orderNo once & increment)
        if (creates.isNotEmpty()) {
            try {
                val firstReq = Gson().fromJson(creates.first().payloadJson, CustomOrderRequest::class.java)
                val nextNoResp = api.getLastOrderNo(ClientCodeRequest(firstReq.ClientCode))
                val lastNo = nextNoResp.body()?.LastOrderNo?.toIntOrNull() ?: return Result.retry()
                var runningNo = lastNo

                creates.forEach { row ->
                    try {
                        val req = Gson().fromJson(row.payloadJson, CustomOrderRequest::class.java)
                        runningNo += 1
                        val newOrderNoStr = runningNo.toString()

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
                            err = e.message ?: "Create error",
                            attempts = row.attempts + 1,
                            serverNo = row.serverOrderNo,
                            serverId = row.serverOrderId
                        )
                    }
                }
            } catch (e: Exception) {
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

}

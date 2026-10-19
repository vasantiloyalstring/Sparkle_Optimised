package com.loyalstring.rfid.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.dao.OrderItemDao
import com.loyalstring.rfid.data.local.dao.PendingEmployeeDao
import com.loyalstring.rfid.data.local.dao.PendingOrderDao
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.DeleteOrderRequest
import com.loyalstring.rfid.ui.utils.OfflineCustomerMatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PendingOrderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: PendingOrderDao,
    private val pendingEmployeeDao: PendingEmployeeDao,
    private val orderItemDao: OrderItemDao,
    private val api: RetrofitInterface,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (pendingEmployeeDao.getPending().isNotEmpty()) {
            Log.d(TAG, "Pending customers still exist — retry order sync later")
            return Result.retry()
        }

        val pending = dao.getAllPending()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false
        val gson = Gson()

        val deletes = pending.filter { it.op.equals("DELETE", true) }
        val updates = pending.filter { it.op.equals("UPDATE", true) }
        val creates = pending.filter { it.op.equals("CREATE", true) || it.op.isBlank() }

        deletes.forEach { row ->
            try {
                val serverId = row.serverOrderId
                if (serverId == null || serverId == 0) {
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

        updates.forEach { row ->
            try {
                val serverId = row.serverOrderId
                if (serverId == null || serverId == 0) {
                    throw Exception("UPDATE missing serverOrderId")
                }
                val req = gson.fromJson(row.payloadJson, CustomOrderRequest::class.java)
                val resp = api.updateCustomerOrder(req)
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

        if (creates.isNotEmpty()) {
            try {
                val firstReq = gson.fromJson(creates.first().payloadJson, CustomOrderRequest::class.java)
                val nextNoResp = api.getLastOrderNo(ClientCodeRequest(firstReq.ClientCode))
                val lastNo = nextNoResp.body()?.LastOrderNo?.toIntOrNull() ?: return Result.retry()
                var runningNo = lastNo

                creates.forEach { row ->
                    try {
                        var req = gson.fromJson(row.payloadJson, CustomOrderRequest::class.java)

                        // Prefer denormalized columns frozen at offline save.
                        val frozenFirst = row.customerFirstName.ifBlank { req.Customer.FirstName }
                        val frozenLast = row.customerLastName.ifBlank { req.Customer.LastName }
                        val frozenMobile = row.customerMobile.ifBlank { req.Customer.Mobile }

                        // Source of truth for server customer id: entity column after employee sync.
                        var customerId = row.customerId.takeIf { it > 0 }
                            ?: req.CustomerId.toIntOrNull()?.takeIf { it > 0 }
                            ?: 0

                        if (customerId <= 0) {
                            req = ensureServerCustomerId(
                                gson = gson,
                                localId = row.localId,
                                req = req.copy(
                                    Customer = req.Customer.copy(
                                        FirstName = frozenFirst,
                                        LastName = frozenLast,
                                        Mobile = frozenMobile,
                                    )
                                ),
                            )
                            customerId = req.CustomerId.toIntOrNull() ?: 0
                        }

                        if (customerId <= 0) {
                            throw Exception(
                                "Customer not synced for order ${row.localId} " +
                                    "key=${row.localCustomerKey} name=$frozenFirst"
                            )
                        }

                        runningNo += 1
                        val newOrderNoStr = runningNo.toString()
                        val display = listOf(frozenFirst, frozenLast)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                        // Build API body from THIS row only — never reuse another order's customer.
                        val newReq = req.copy(
                            OrderNo = newOrderNoStr,
                            CustomerId = customerId.toString(),
                            Customer = req.Customer.copy(
                                Id = customerId,
                                FirstName = frozenFirst,
                                LastName = frozenLast,
                                Mobile = frozenMobile,
                                Remark = OfflineCustomerMatch.mergeRemark(
                                    req.Customer.Remark,
                                    row.localCustomerKey.ifBlank { null },
                                ),
                            ),
                            CustomOrderItem = req.CustomOrderItem.map {
                                it.copy(
                                    CustomerId = customerId,
                                    CustomerName = display.ifBlank { it.CustomerName },
                                )
                            },
                        )

                        Log.d(
                            TAG,
                            "POST order local=${row.localId} orderNo=$newOrderNoStr " +
                                "customerId=$customerId name=$display key=${row.localCustomerKey}"
                        )

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

    private suspend fun ensureServerCustomerId(
        gson: Gson,
        localId: String,
        req: CustomOrderRequest,
    ): CustomOrderRequest {
        val existing = req.CustomerId.toIntOrNull() ?: 0
        if (existing > 0) return req

        val clientCode = req.ClientCode.orEmpty()
        if (clientCode.isBlank()) return req

        val mobile = req.Customer.Mobile.trim()
        val firstName = req.Customer.FirstName.trim()
        val lastName = req.Customer.LastName.trim()

        // Exact mobile only; then exact first+last. No blank-name fallbacks.
        val found = when {
            mobile.isNotEmpty() ->
                orderItemDao.findCustomerByMobile(clientCode, mobile)
                    ?.takeIf { (it.Id ?: 0) > 0 }
            firstName.isNotEmpty() && lastName.isNotEmpty() ->
                orderItemDao.findServerCustomerByName(clientCode, firstName, lastName)
            else -> null
        }

        val serverId = found?.Id?.takeIf { it > 0 } ?: return req

        val updated = req.copy(
            CustomerId = serverId.toString(),
            Customer = req.Customer.copy(Id = serverId),
            CustomOrderItem = req.CustomOrderItem.map { it.copy(CustomerId = serverId) },
        )
        dao.updatePayloadWithCustomer(
            localId = localId,
            payloadJson = gson.toJson(updated),
            customerId = serverId,
            op = "CREATE",
        )
        return updated
    }

    companion object {
        private const val TAG = "PendingOrderSync"
    }
}

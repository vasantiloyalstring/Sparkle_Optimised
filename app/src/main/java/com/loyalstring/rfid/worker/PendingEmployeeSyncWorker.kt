package com.loyalstring.rfid.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.dao.PendingEmployeeDao
import com.loyalstring.rfid.data.local.dao.PendingOrderDao
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.repository.OrderRepository
import com.loyalstring.rfid.ui.utils.OfflineCustomerMatch
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sync offline customers, then stamp the matching pending orders with the
 * server CustomerId using [PendingOrderEntity.localCustomerKey] == employee localId.
 * Never remaps by shared name matching.
 */
@HiltWorker
class PendingEmployeeSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingEmployeeDao: PendingEmployeeDao,
    private val pendingOrderDao: PendingOrderDao,
    private val repository: OrderRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending = pendingEmployeeDao.getPending()
        if (pending.isEmpty()) return Result.success()

        val gson = Gson()
        var anyFailed = false

        for (row in pending) {
            try {
                val req = gson.fromJson(row.payloadJson, AddEmployeeRequest::class.java)
                // Also push to JewelMarts when offline customer syncs online.
                runCatching { repository.pushClientAddToJewelMarts(req) }
                val response = repository.AAddAllEmployeeDetails(req)
                val body = response.body()

                if (response.isSuccessful && body != null && body.message.isNullOrBlank() && body.Id > 0) {
                    applyServerCustomerIdToOrders(
                        gson = gson,
                        localCustomerKey = row.localId,
                        serverCustomerId = body.Id,
                    )
                    pendingEmployeeDao.markSynced(row.localId)
                    pendingEmployeeDao.deleteByLocalId(row.localId)
                } else if (response.isSuccessful && !body?.message.isNullOrBlank()) {
                    Log.w(TAG, "Customer sync logical error: ${body?.message}")
                    pendingEmployeeDao.markFailed(row.localId, body?.message)
                    anyFailed = true
                } else {
                    pendingEmployeeDao.markFailed(row.localId, "HTTP ${response.code()}")
                    anyFailed = true
                }
            } catch (e: Exception) {
                pendingEmployeeDao.markFailed(row.localId, e.message)
                anyFailed = true
            }
        }

        pending.map { it.clientCode }.distinct().filter { it.isNotBlank() }.forEach { code ->
            runCatching { repository.getAllEmpList(ClientCodeRequest(code)) }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    private suspend fun applyServerCustomerIdToOrders(
        gson: Gson,
        localCustomerKey: String,
        serverCustomerId: Int,
    ) {
        if (localCustomerKey.isBlank() || serverCustomerId <= 0) return

        // 1) Column stamp — source of truth for order sync.
        pendingOrderDao.setServerCustomerIdByLocalKey(localCustomerKey, serverCustomerId)

        // 2) Update each linked payload so API body also has the correct CustomerId / names.
        val linked = pendingOrderDao.getPendingCreatesByLocalCustomerKey(localCustomerKey)
        for (orderRow in linked) {
            try {
                val req = gson.fromJson(orderRow.payloadJson, CustomOrderRequest::class.java)
                val first = orderRow.customerFirstName.ifBlank { req.Customer.FirstName }
                val last = orderRow.customerLastName.ifBlank { req.Customer.LastName }
                val mobile = orderRow.customerMobile.ifBlank { req.Customer.Mobile }
                val display = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")

                val updatedCustomer = req.Customer.copy(
                    Id = serverCustomerId,
                    FirstName = first,
                    LastName = last,
                    Mobile = mobile,
                    Remark = OfflineCustomerMatch.mergeRemark(req.Customer.Remark, localCustomerKey),
                )
                val updatedItems = req.CustomOrderItem.map { item ->
                    item.copy(
                        CustomerId = serverCustomerId,
                        CustomerName = display.ifBlank { item.CustomerName },
                    )
                }
                val updatedReq = req.copy(
                    CustomerId = serverCustomerId.toString(),
                    Customer = updatedCustomer,
                    CustomOrderItem = updatedItems,
                )
                pendingOrderDao.updatePayloadWithCustomer(
                    localId = orderRow.localId,
                    payloadJson = gson.toJson(updatedReq),
                    customerId = serverCustomerId,
                    op = orderRow.op.ifBlank { "CREATE" },
                )
                Log.d(
                    TAG,
                    "Linked order ${orderRow.localId} -> serverCustomerId=$serverCustomerId " +
                        "via localCustomerKey=$localCustomerKey name=$display"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating payload for ${orderRow.localId}: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "PendingEmployeeSync"
    }
}

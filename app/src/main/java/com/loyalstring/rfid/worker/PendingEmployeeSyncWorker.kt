package com.loyalstring.rfid.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.dao.PendingEmployeeDao
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.repository.OrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PendingEmployeeSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingEmployeeDao: PendingEmployeeDao,
    private val repository: OrderRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending = pendingEmployeeDao.getPending()
        if (pending.isEmpty()) return Result.success()

        val gson = Gson()

        for (row in pending) {
            try {
                val req = gson.fromJson(row.payloadJson, AddEmployeeRequest::class.java)

                val response = repository.AAddAllEmployeeDetails(req)
                if (response.isSuccessful) {
                    // ✅ remove pending entry
                    pendingEmployeeDao.markSynced(row.localId)
                    pendingEmployeeDao.deleteByLocalId(row.localId)

                    // ✅ optional but recommended: server se fresh list kheench ke room update
                    repository.getAllEmpList(ClientCodeRequest(row.clientCode.toString()))

                } else {
                    pendingEmployeeDao.markFailed(row.localId, "HTTP ${response.code()}")
                    // retry later
                    return Result.retry()
                }
            } catch (e: Exception) {
                pendingEmployeeDao.markFailed(row.localId, e.message)
                return Result.retry()
            }
        }

        return Result.success()
    }
}

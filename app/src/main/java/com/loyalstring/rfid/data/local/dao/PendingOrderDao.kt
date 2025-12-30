package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.PendingOrderEntity
@Dao
interface PendingOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingOrderEntity)

    @Query("SELECT * FROM pending_orders WHERE status='PENDING' ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingOrderEntity>

    @Query("UPDATE pending_orders SET status=:status, lastError=:err, attempts=:attempts, serverOrderNo=:serverNo, serverOrderId=:serverId WHERE localId=:localId")
    suspend fun updateStatus(
        localId: String,
        status: String,
        err: String?,
        attempts: Int,
        serverNo: String?,
        serverId: Int?
    )


}
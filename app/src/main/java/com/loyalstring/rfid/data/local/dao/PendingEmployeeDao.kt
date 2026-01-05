package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.loyalstring.rfid.data.local.entity.PendingEmployeeEntity

@Dao
interface PendingEmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingEmployeeEntity)

    @Query("SELECT * FROM pending_employee WHERE status='PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingEmployeeEntity>

    @Query("DELETE FROM pending_employee WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    @Query("UPDATE pending_employee SET status='FAILED', lastError=:err WHERE localId=:localId")
    suspend fun markFailed(localId: String, err: String?)

    @Query("UPDATE pending_employee SET status='SYNCED' WHERE localId=:localId")
    suspend fun markSynced(localId: String)
}

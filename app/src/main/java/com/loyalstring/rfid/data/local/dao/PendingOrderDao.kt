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

    @Query("SELECT * FROM pending_orders WHERE clientCode = :clientCode AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingByClientCode(clientCode: String): List<PendingOrderEntity>

    @Query("SELECT * FROM pending_orders WHERE clientCode = :clientCode AND status='PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOrders(clientCode: String): List<PendingOrderEntity>

    @Query("DELETE FROM pending_orders WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    @Query("SELECT * FROM pending_orders WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): PendingOrderEntity?

    @Query("""
    UPDATE pending_orders
    SET payloadJson = :payloadJson,
        op = :op,
        status = 'PENDING',
        attempts = 0,
        lastError = NULL,
        updatedAt = :updatedAt
    WHERE localId = :localId
""")
    suspend fun updatePayload(
        localId: String,
        payloadJson: String,
        op: String,
        updatedAt: Long = System.currentTimeMillis()
    )


    // ✅ Offline delete mark
    @Query("""
        UPDATE pending_orders
        SET 
            isDeleted = 1,
            op = 'DELETE',
            status = 'PENDING',
            updatedAt = :ts
        WHERE localId = :localId
    """)
    suspend fun markPendingDelete(localId: String, ts: Long = System.currentTimeMillis())

    // ✅ Hard delete local row (after server delete success OR offline-created delete)
    @Query("DELETE FROM pending_orders WHERE localId = :localId")
    suspend fun hardDeleteByLocalId(localId: String)

    // ✅ Fetch all pending deletes to sync when internet available
    @Query("""
        SELECT * FROM pending_orders
        WHERE op = 'DELETE' AND status = 'PENDING'
        ORDER BY updatedAt ASC
    """)
    suspend fun getPendingDeletes(): List<PendingOrderEntity>

    // ✅ Mark as synced (if you choose to keep row; usually we hard-delete after delete sync)
    @Query("""
        UPDATE pending_orders
        SET 
            status = 'SYNCED',
            lastError = NULL
        WHERE localId = :localId
    """)
    suspend fun markSynced(localId: String)

    // ✅ Optional: mark failed + increment attempts
    @Query("""
        UPDATE pending_orders
        SET 
            status = 'FAILED',
            lastError = :error,
            attempts = attempts + 1,
            updatedAt = :ts
        WHERE localId = :localId
    """)
    suspend fun markFailed(localId: String, error: String, ts: Long = System.currentTimeMillis())

    // ✅ Optional: reset failed to pending (retry)
    @Query("""
        UPDATE pending_orders
        SET 
            status = 'PENDING',
            lastError = NULL,
            updatedAt = :ts
        WHERE localId = :localId
    """)
    suspend fun markPending(localId: String, ts: Long = System.currentTimeMillis())





}
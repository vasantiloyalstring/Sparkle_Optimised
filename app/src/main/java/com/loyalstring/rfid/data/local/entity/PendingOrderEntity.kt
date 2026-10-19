package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey val localId: String,
    val clientCode: String,
    /** Server customer id once known; 0 while waiting for offline customer sync. */
    val customerId: Int,
    val payloadJson: String,
    val status: String,
    val createdAt: Long,
    val lastError: String? = null,
    val attempts: Int = 0,
    val serverOrderNo: String? = null,
    val serverOrderId: Int? = null,
    val op: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    /**
     * Hard link to offline [PendingEmployeeEntity.localId].
     * Sync must map server CustomerId using this key only — never by shared name matching.
     */
    val localCustomerKey: String = "",
    /** Frozen at offline save — used to keep payload names correct during sync. */
    val customerFirstName: String = "",
    val customerLastName: String = "",
    val customerMobile: String = "",
)

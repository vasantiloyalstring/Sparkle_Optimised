package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey val localId: String,            // UUID
    val clientCode: String,
    val customerId: Int,
    val payloadJson: String,                     // CustomOrderRequest as JSON (items included)
    val status: String,                          // PENDING / SYNCED / FAILED
    val createdAt: Long,
    val lastError: String? = null,
    val attempts: Int = 0,
    val serverOrderNo: String? = null,
    val serverOrderId: Int? = null
)

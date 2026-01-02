package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_employee")
data class PendingEmployeeEntity(
    @PrimaryKey val localId: String,
    val clientCode: String,
    val payloadJson: String,
    val status: String = "PENDING",  // PENDING / SYNCED / FAILED
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null
)

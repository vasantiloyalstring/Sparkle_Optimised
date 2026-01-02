package com.loyalstring.rfid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_list_cache")
data class OrderListCacheEntity(
    @PrimaryKey val orderId: Int,

    @ColumnInfo(name = "clientCode")
    val ClientCode: String,

    @ColumnInfo(name = "createdAt")
    val created_at: Long,

    val payloadJson: String
)

package com.loyalstring.rfid.data.model.order

import androidx.room.Embedded
import androidx.room.Relation
import com.loyalstring.rfid.data.local.entity.BulkItem

data class BulkItemWithDetails( @Embedded
                                val bulkItem: BulkItem,

                                @Relation(
                                    parentColumn = "id",
                                    entityColumn = "bulkItemId"
                                )
                                val stones: List<Stone>,

                                @Relation(
                                    parentColumn = "id",
                                    entityColumn = "bulkItemId"
                                )
                                val diamonds: List<Diamond>)

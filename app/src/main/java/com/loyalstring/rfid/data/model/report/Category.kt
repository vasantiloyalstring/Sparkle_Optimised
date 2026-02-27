package com.loyalstring.rfid.data.model.report

data class Category( val CategoryId: Int?,
                     val CategoryName: String?,
                     val TotalInventoryItems: Int?,
                     val TotalScannedItems: Int?,
                     val NotScannedItems: Int?,
                     val MatchedQty: Int?,
                     val UnmatchQty: Int?,
                     val MatchWeight: Double?,
                     val UnmatchWeight: Double?,
                     val Products: List<Product>?)

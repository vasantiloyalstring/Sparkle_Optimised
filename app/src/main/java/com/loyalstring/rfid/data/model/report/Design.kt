package com.loyalstring.rfid.data.model.report

data class Design( val DesignId: Int?,
                   val DesignName: String?,
                   val TotalInventoryItems: Int?,
                   val TotalScannedItems: Int?,
                   val NotScannedItems: Int?,
                   val MatchedQty: Int?,
                   val UnmatchQty: Int?,
                   val MatchWeight: Double?,
                   val UnmatchWeight: Double?,
                   val Items: List<Item>?)

package com.loyalstring.rfid.data.model.report

data class Product( val ProductId: Int?,
                    val ProductName: String?,
                    val TotalInventoryItems: Int?,
                    val TotalScannedItems: Int?,
                    val NotScannedItems: Int?,
                    val MatchedQty: Int?,
                    val UnmatchQty: Int?,
                    val MatchWeight: Double?,
                    val UnmatchWeight: Double?,
                    val Designs: List<Design>?)

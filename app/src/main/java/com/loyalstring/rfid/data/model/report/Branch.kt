package com.loyalstring.rfid.data.model.report

data class Branch(  val BranchId: Int?,
                    val BranchName: String?,
                    val TotalInventoryItems: Int?,
                    val TotalScannedItems: Int?,
                    val NotScannedItems: Int?,
                    val MatchedQty: Int?,
                    val UnmatchQty: Int?,
                    val MatchWeight: Double?,
                    val UnmatchWeight: Double?,
                    val Categories: List<Category>?)

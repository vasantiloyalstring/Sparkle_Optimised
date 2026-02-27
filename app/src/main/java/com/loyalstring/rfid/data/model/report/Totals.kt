package com.loyalstring.rfid.data.model.report

data class Totals(    val TotalInventoryQty: Int?,
                      val TotalInventoryWeight: Double?,
                      val TotalScannedItems: Int?,
                      val TotalNotScannedItems: Int?,
                      val MatchedQty: Int?,
                      val UnmatchQty: Int?,
                      val TotalMatchWeight: Double?,
                      val TotalUnmatchWeight: Double?)

package com.loyalstring.rfid.data.model.stockVerification

data class TotalsModel(  val TotalQty: Int = 0,
                         val TotalGrossWeight: Double = 0.0,
                         val TotalNetWeight: Double = 0.0,

                         val TotalMatchQty: Int = 0,
                         val TotalUnmatchQty: Int = 0,

                         val TotalMatchGrossWeight: Double = 0.0,
                         val TotalMatchNetWeight: Double = 0.0,

                         val TotalUnmatchGrossWeight: Double = 0.0,
                         val TotalUnmatchNetWeight: Double = 0.0)

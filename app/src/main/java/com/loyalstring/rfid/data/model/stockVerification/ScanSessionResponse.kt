package com.loyalstring.rfid.data.model.stockVerification

data class ScanSessionResponse(val Message: String? = null,
                               val ScanBatchId: String? = null,
                               val MATCH: List<StockItemModel>? = null,
                               val UNMATCH: List<StockItemModel>? = null,
                               val Totals: TotalsModel? = null)
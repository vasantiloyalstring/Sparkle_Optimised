package com.loyalstring.rfid.data.model.deliveryChallan

data class DeliveryChallanItemPrint(  val itemName: String,
                                      val purity: String,
                                      val pcs: Int,
                                      val grossWt: String,
                                      val stoneWt: String,
                                      val netWt: String,
                                      val ratePerGram: String,
                                      val wastage: String,
                                      val itemAmount: String)

data class DeliveryChallanPrintData(
    val branchName: String,
    val city: String,
    val createdDateTime: String,
    val customerName: String,
    val quotationNo: String,
    val phone: String,
    val items: List<DeliveryChallanItemPrint>,
    val taxableAmount: String,
    val cgstPercent: Double,
    val cgstAmount: String,
    val sgstPercent: Double,
    val sgstAmount: String,
    val totalNetAmount: String
)

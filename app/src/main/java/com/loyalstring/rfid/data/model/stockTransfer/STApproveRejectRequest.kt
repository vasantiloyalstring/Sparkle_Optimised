package com.loyalstring.rfid.data.model.stockTransfer

data class STApproveRejectRequest(val StockTransferItems: List<STApproveRejectItem>,
                                  val ClientCode: String,
                                  val UserID: String,
                                  val RequestTyp: String
)

package com.loyalstring.rfid.data.model.stockVerification

data class StockItemModel(  val ItemCode: String? = null,
                            val ClientCode: String? = null,
                            val Status: String? = null,

                            val CounterId: Int? = null,
                            val CategoryId: Int? = null,
                            val ProductId: Int? = null,
                            val DesignId: Int? = null,
                            val PurityId: Int? = null,
                            val CompanyId: Int? = null,
                            val BranchId: Int? = null,

                            val CounterName: String? = null,
                            val CategoryName: String? = null,
                            val ProductName: String? = null,
                            val DesignName: String? = null,
                            val PurityName: String? = null,
                            val CompanyName: String? = null,
                            val BranchName: String? = null,

                            val GrossWeight: Double? = null,
                            val NetWeight: Double? = null,
                            val Quantity: Int? = null,

                            val TIDNumber: String? = null,
                            val RFIDCode: String? = null,
                            val BoxName: String? = null,

                            val CreatedOn: String? = null,
                            val LastUpdated: String? = null,
                            val ScanBatchId: String? = null,

                            val Id: Int? = null,
                            val StatusType: Boolean? = null)

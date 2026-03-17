package com.loyalstring.rfid.data.remote.data

data class StockTransferRequest(
    val ClientCode: String,
    val StockTransferItems: List<StockTransferItemData>,
    val StockType: String,
    val StockTransferTypeName: String,
    val TransferTypeId: Int,
    val TransferByEmployee: String,
    val TransferedToBranch: String,
    val TransferToEmployee: String,
    val TransferedBranch: String,
    val Source: Int,
    val Destination: Int,
    val Remarks: String,
    val StockTransferDate: String,
    val ReceivedByEmployee: String
)

data class StockTransferItemData(
    val stockId: Int
)

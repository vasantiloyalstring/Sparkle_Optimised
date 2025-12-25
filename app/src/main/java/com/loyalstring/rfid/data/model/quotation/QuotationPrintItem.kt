package com.loyalstring.rfid.data.model.quotation

data class QuotationPrintItem(
    val imageUrl: String? = null,
    val particulars: String,
    val grossWt: String?,
    val netWt: String?,
    val qty: String?,
    val ratePerGm: String?,
    val makingPerGm: String?,
    val amount: String?
)

data class QuotationPrintData(
    val ownerName: String,
    val ownerAddress: String,
    val ownerContact: String,

    val quotationNo: String,
    val date: String,
    val salesMan: String? = "",
    val remark: String? = "",

    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,

    val items: List<QuotationPrintItem>,

    val totalAmount: String,
    val cgst: String = "0.00",
    val sgst: String = "0.00",
    val igst: String = "0.00"
)

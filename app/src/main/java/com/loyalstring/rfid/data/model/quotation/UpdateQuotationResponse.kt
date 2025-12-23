package com.loyalstring.rfid.data.model.quotation

import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.order.Customer

data class UpdateQuotationResponse(@SerializedName("Id") val id: Int? = null,
                                   @SerializedName("CustomerId") val customerId: Int? = null,
                                   @SerializedName("VendorId") val vendorId: Int? = null,

                                   @SerializedName("TotalAmount") val totalAmount: String? = null,
                                   @SerializedName("PaymentMode") val paymentMode: String? = null,
                                   @SerializedName("Offer") val offer: String? = null,
                                   @SerializedName("Qty") val qty: String? = null,

                                   @SerializedName("GST") val gst: String? = null,
                                   @SerializedName("ReceivedAmount") val receivedAmount: String? = null,

                                   @SerializedName("QuotationStatus") val quotationStatus: String? = null,
                                   @SerializedName("Visibility") val visibility: String? = null,

                                   @SerializedName("MRP") val mrp: String? = null,
                                   @SerializedName("GrossWt") val grossWt: String? = null,
                                   @SerializedName("NetWt") val netWt: String? = null,
                                   @SerializedName("StoneWt") val stoneWt: String? = null,
                                   @SerializedName("CuttingNetWt") val cuttingNetWt: String? = null,

                                   @SerializedName("CategoryId") val categoryId: Int? = null,
                                   @SerializedName("DeliveryAddress") val deliveryAddress: String? = null,
                                   @SerializedName("BillType") val billType: String? = null,
                                   @SerializedName("UrdPurchaseAmt") val urdPurchaseAmt: String? = null,

                                   @SerializedName("BilledBy") val billedBy: String? = null,
                                   @SerializedName("SoldBy") val soldBy: String? = null,

                                   @SerializedName("QuotationNo") val quotationNo: String? = null,
                                   @SerializedName("Date") val date: String? = null,

                                   @SerializedName("AdvanceAmt") val advanceAmt: String? = null,
                                   @SerializedName("MobileNo") val mobileNo: String? = null,
                                   @SerializedName("CustomerName") val customerName: String? = null,

                                   @SerializedName("CreditSilver") val creditSilver: String? = null,
                                   @SerializedName("CreditGold") val creditGold: String? = null,
                                   @SerializedName("CreditAmount") val creditAmount: String? = null,
                                   @SerializedName("BalanceAmt") val balanceAmt: String? = null,

                                   @SerializedName("TotalSaleGold") val totalSaleGold: String? = null,
                                   @SerializedName("TotalSaleSilver") val totalSaleSilver: String? = null,
                                   @SerializedName("TotalSaleUrdGold") val totalSaleUrdGold: String? = null,
                                   @SerializedName("TotalSaleUrdSilver") val totalSaleUrdSilver: String? = null,

                                   @SerializedName("SaleType") val saleType: String? = null,
                                   @SerializedName("FinancialYear") val financialYear: String? = null,
                                   @SerializedName("BaseCurrency") val baseCurrency: String? = null,

                                   @SerializedName("TotalNetAmount") val totalNetAmount: String? = null,
                                   @SerializedName("TotalGSTAmount") val totalGstAmount: String? = null,
                                   @SerializedName("TotalPurchaseAmount") val totalPurchaseAmount: String? = null,

                                   @SerializedName("PurchaseStatus") val purchaseStatus: String? = null,
                                   @SerializedName("GSTApplied") val gstApplied: String? = null,
                                   @SerializedName("TDS") val tds: String? = null,
                                   @SerializedName("CourierCharge") val courierCharge: String? = null,

                                   @SerializedName("AdditionTaxApplied") val additionTaxApplied: String? = null,
                                   @SerializedName("Discount") val discount: String? = null,

                                   @SerializedName("TotalBalanceMetal") val totalBalanceMetal: String? = null,
                                   @SerializedName("BalanceAmount") val balanceAmount: String? = null,
                                   @SerializedName("TotalFineMetal") val totalFineMetal: String? = null,

                                   @SerializedName("TotalStoneWeight") val totalStoneWeight: String? = null,
                                   @SerializedName("TotalStoneAmount") val totalStoneAmount: String? = null,
                                   @SerializedName("TotalStonePieces") val totalStonePieces: String? = null,

                                   @SerializedName("TotalDiamondWeight") val totalDiamondWeight: String? = null,
                                   @SerializedName("TotalDiamondPieces") val totalDiamondPieces: String? = null,
                                   @SerializedName("TotalDiamondAmount") val totalDiamondAmount: String? = null,

                                   @SerializedName("ClientCode") val clientCode: String? = null,
                                   @SerializedName("QuotationCount") val quotationCount: String? = null,

                                   @SerializedName("CreatedOn") val createdOn: String? = null,
                                   @SerializedName("LastUpdated") val lastUpdated: String? = null,

                                   @SerializedName("CompanyId") val companyId: Int? = null,
                                   @SerializedName("BranchId") val branchId: Int? = null,
                                   @SerializedName("CounterId") val counterId: Int? = null,
                                   @SerializedName("EmployeeId") val employeeId: Int? = null,

                                   @SerializedName("FineSilver") val fineSilver: String? = null,
                                   @SerializedName("FineGold") val fineGold: String? = null,
                                   @SerializedName("DebitSilver") val debitSilver: String? = null,
                                   @SerializedName("DebitGold") val debitGold: String? = null,
                                   @SerializedName("BalanceSilver") val balanceSilver: String? = null,
                                   @SerializedName("BalanceGold") val balanceGold: String? = null,

                                   @SerializedName("QuotationDate") val quotationDate: String? = null,
                                   @SerializedName("TotalQuotationCount") val totalQuotationCount: String? = null,

                                   @SerializedName("FirstName") val firstName: String? = null,
                                   @SerializedName("LastName") val lastName: String? = null,
                                   @SerializedName("Mobile") val mobile: String? = null,
                                   @SerializedName("Email") val email: String? = null,

                                   @SerializedName("QuotationItem") val quotationItem: List<QuotationItem>? = emptyList(),
                                   @SerializedName("Customer") val customer: Customer? = null,

                                   @SerializedName("QuotationStoneDetails") val quotationStoneDetails: Any? = null,
                                   @SerializedName("QuotationDiamondDetails") val quotationDiamondDetails: Any? = null)

package com.loyalstring.rfid.data.model.quotation

import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.order.Customer

data class UpdateQuotationRequest(@SerializedName("AdditionTaxApplied") val additionTaxApplied: String? = null,
                                  @SerializedName("additionalTax") val additionalTax: String? = null,                // UI
                                  @SerializedName("additionalTaxCheckbox") val additionalTaxCheckbox: Boolean? = null, // UI

                                  @SerializedName("AdvanceAmt") val advanceAmt: String? = null,
                                  @SerializedName("BalanceAmount") val balanceAmount: String? = null,

                                  @SerializedName("balanceGold") val balanceGold: String? = null,
                                  @SerializedName("BalanceSilver") val balanceSilver: String? = null,
                                  @SerializedName("balanceamount") val balanceamount: String? = null,

                                  @SerializedName("BaseCurrency") val baseCurrency: String? = null,
                                  @SerializedName("BillType") val billType: String? = null,
                                  @SerializedName("BilledBy") val billedBy: String? = null,

                                  @SerializedName("BranchId") val branchId: Int? = null,
                                  @SerializedName("CategoryId") val categoryId: Int? = null,
                                  @SerializedName("ClientCode") val clientCode: String? = null,
                                  @SerializedName("CompanyId") val companyId: Int? = null,
                                  @SerializedName("CounterId") val counterId: Int? = null,

                                  @SerializedName("CourierCharge") val courierCharge: String? = null,
                                  @SerializedName("CreditAmount") val creditAmount: String? = null,
                                  @SerializedName("CreditGold") val creditGold: String? = null,
                                  @SerializedName("CreditSilver") val creditSilver: String? = null,

                                  @SerializedName("Customer") val customer: Customer? = null,
                                  @SerializedName("CustomerId") val customerId: Int? = null,

                                  @SerializedName("CuttingNetWt") val cuttingNetWt: String? = null,
                                  @SerializedName("Date") val date: String? = null,

                                  @SerializedName("DeliveryAddress") val deliveryAddress: String? = null,
                                  @SerializedName("Discount") val discount: String? = null,

                                  @SerializedName("Email") val email: String? = null,
                                  @SerializedName("EmployeeId") val employeeId: Int? = null,

                                  @SerializedName("FinancialYear") val financialYear: String? = null,
                                  @SerializedName("GSTApplied") val gstApplied: String? = null,
                                  @SerializedName("gstCheckbox") val gstCheckbox: Boolean? = null, // UI

                                  @SerializedName("GrossWt") val grossWt: String? = null,

                                  @SerializedName("Id") val id: Int? = null,
                                  @SerializedName("LastName") val lastName: String? = null,

                                  @SerializedName("MRP") val mrp: String? = null,
                                  @SerializedName("Offer") val offer: String? = null,

                                  @SerializedName("PaymentMode") val paymentMode: String? = null,
                                  @SerializedName("PurchaseStatus") val purchaseStatus: String? = null,

                                  @SerializedName("Qty") val qty: String? = null,

                                  @SerializedName("QuotationCount") val quotationCount: String? = null,
                                  @SerializedName("QuotationDate") val quotationDate: String? = null,

                                  @SerializedName("QuotationItem") val quotationItem: List<QuotationItem>? = emptyList(),

                                  @SerializedName("QuotationNo") val quotationNo: String? = null,
                                  @SerializedName("QuotationStatus") val quotationStatus: String? = null,

                                  @SerializedName("ReceivedAmount") val receivedAmount: String? = null,
                                  @SerializedName("SaleType") val saleType: String? = null,
                                  @SerializedName("SoldBy") val soldBy: String? = null,

                                  @SerializedName("StoneWt") val stoneWt: String? = null,
                                  @SerializedName("TDS") val tds: String? = null,

                                  @SerializedName("TotalAmount") val totalAmount: String? = null,
                                  @SerializedName("TotalBalanceMetal") val totalBalanceMetal: String? = null,

                                  @SerializedName("TotalDiamondAmount") val totalDiamondAmount: String? = null,
                                  @SerializedName("TotalDiamondPieces") val totalDiamondPieces: String? = null,
                                  @SerializedName("TotalDiamondWeight") val totalDiamondWeight: String? = null,

                                  @SerializedName("TotalGSTAmount") val totalGstAmount: String? = null,
                                  @SerializedName("TotalNetAmount") val totalNetAmount: String? = null,
                                  @SerializedName("TotalPurchaseAmount") val totalPurchaseAmount: String? = null,

                                  @SerializedName("TotalSaleGold") val totalSaleGold: String? = null,
                                  @SerializedName("TotalSaleSilver") val totalSaleSilver: String? = null,
                                  @SerializedName("TotalSaleUrdGold") val totalSaleUrdGold: String? = null,
                                  @SerializedName("TotalSaleUrdSilver") val totalSaleUrdSilver: String? = null,

                                  @SerializedName("TotalStoneAmount") val totalStoneAmount: String? = null,
                                  @SerializedName("TotalStonePieces") val totalStonePieces: String? = null,
                                  @SerializedName("TotalStoneWeight") val totalStoneWeight: String? = null,

                                  @SerializedName("TotalTaxableAmount") val totalTaxableAmount: String? = null,
                                  @SerializedName("UrdPurchaseAmt") val urdPurchaseAmt: String? = null,

                                  @SerializedName("VendorId") val vendorId: Int? = null,
                                  @SerializedName("Visibility") val visibility: String? = null)

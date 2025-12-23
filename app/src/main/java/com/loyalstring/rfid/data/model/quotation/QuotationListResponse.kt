package com.loyalstring.rfid.data.model.quotation

import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.order.Customer

data class QuotationListResponse(  @SerializedName("Id") val id: Int? = null,
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
                                   @SerializedName("TotalGSTAmount") val totalGSTAmount: String? = null,
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

                                   @SerializedName("QuotationItem") val quotationItem: List<QuotationItem> = emptyList(),
                                   @SerializedName("Customer") val customer: Customer? = null,

    // not provided structure -> keep Any for safety
                                   @SerializedName("QuotationStoneDetails") val quotationStoneDetails: List<Any> = emptyList(),
                                   @SerializedName("QuotationDiamondDetails") val quotationDiamondDetails: List<Any> = emptyList()
)

/*data class QuotationItem(
    @SerializedName("QuotationItemId") val quotationItemId: Int? = null,
    @SerializedName("MRP") val mrp: String? = null,
    @SerializedName("Image") val image: String? = null,
    @SerializedName("StoneLessPercent") val stoneLessPercent: String? = null,

    @SerializedName("CategoryName") val categoryName: String? = null,
    @SerializedName("QuotationStatus") val quotationStatus: String? = null,
    @SerializedName("ProductName") val productName: String? = null,

    @SerializedName("Quantity") val quantity: String? = null,
    @SerializedName("HSNCode") val hsnCode: String? = null,
    @SerializedName("ItemCode") val itemCode: String? = null,

    @SerializedName("GrossWt") val grossWt: String? = null,
    @SerializedName("NetWt") val netWt: String? = null,

    @SerializedName("ProductId") val productId: Int? = null,
    @SerializedName("CustomerId") val customerId: Int? = null,

    @SerializedName("MetalRate") val metalRate: String? = null,
    @SerializedName("MakingCharg") val makingCharg: String? = null,
    @SerializedName("Price") val price: String? = null,

    @SerializedName("HUIDCode") val huidCode: String? = null,
    @SerializedName("ProductCode") val productCode: String? = null,
    @SerializedName("ProductNo") val productNo: String? = null,
    @SerializedName("Size") val size: String? = null,

    @SerializedName("StoneAmount") val stoneAmount: String? = null,
    @SerializedName("TotalWt") val totalWt: String? = null,

    @SerializedName("PackingWeight") val packingWeight: String? = null,
    @SerializedName("MetalAmount") val metalAmount: String? = null,
    @SerializedName("OldGoldPurchase") val oldGoldPurchase: String? = null,
    @SerializedName("RatePerGram") val ratePerGram: String? = null,
    @SerializedName("Amount") val amount: String? = null,

    @SerializedName("BillType") val billType: String? = null,

    @SerializedName("PurProductNetWt") val purProductNetWt: String? = null,
    @SerializedName("PurProductGrossWt") val purProductGrossWt: String? = null,
    @SerializedName("PurProductAmt") val purProductAmt: String? = null,
    @SerializedName("PurProductId") val purProductId: Int? = null,

    @SerializedName("UnlProductNetWt") val unlProductNetWt: String? = null,
    @SerializedName("UnlProductGrossWt") val unlProductGrossWt: String? = null,
    @SerializedName("UnlProductAmt") val unlProductAmt: String? = null,
    @SerializedName("UnlProductId") val unlProductId: Int? = null,

    @SerializedName("FinePercentage") val finePercentage: String? = null,
    @SerializedName("PurchaseInvoiceNo") val purchaseInvoiceNo: String? = null,

    @SerializedName("HallmarkAmount") val hallmarkAmount: String? = null,
    @SerializedName("HallmarkNo") val hallmarkNo: String? = null,

    @SerializedName("MakingFixedAmt") val makingFixedAmt: String? = null,
    @SerializedName("MakingFixedWastage") val makingFixedWastage: String? = null,
    @SerializedName("MakingPerGram") val makingPerGram: String? = null,
    @SerializedName("MakingPercentage") val makingPercentage: String? = null,

    @SerializedName("Description") val description: String? = null,

    @SerializedName("CuttingGrossWt") val cuttingGrossWt: String? = null,
    @SerializedName("CuttingNetWt") val cuttingNetWt: String? = null,

    @SerializedName("BaseCurrency") val baseCurrency: String? = null,

    @SerializedName("CategoryId") val categoryId: Int? = null,
    @SerializedName("PurityId") val purityId: Int? = null,

    @SerializedName("TotalStoneWeight") val totalStoneWeight: String? = null,
    @SerializedName("TotalStoneAmount") val totalStoneAmount: String? = null,
    @SerializedName("TotalStonePieces") val totalStonePieces: String? = null,

    @SerializedName("TotalDiamondWeight") val totalDiamondWeight: String? = null,
    @SerializedName("TotalDiamondPieces") val totalDiamondPieces: String? = null,
    @SerializedName("TotalDiamondAmount") val totalDiamondAmount: String? = null,

    @SerializedName("SKUId") val skuId: Int? = null,
    @SerializedName("SKU") val sku: String? = null,

    @SerializedName("FineWastageWt") val fineWastageWt: String? = null,
    @SerializedName("TotalItemAmount") val totalItemAmount: String? = null,
    @SerializedName("Pieces") val pieces: String? = null,

    @SerializedName("DesignId") val designId: Int? = null,
    @SerializedName("DesignName") val designName: String? = null,

    @SerializedName("ClientCode") val clientCode: String? = null,

    @SerializedName("DiamondSize") val diamondSize: String? = null,
    @SerializedName("DiamondWeight") val diamondWeight: String? = null,

    @SerializedName("DiamondPurchaseRate") val diamondPurchaseRate: String? = null,
    @SerializedName("DiamondSellRate") val diamondSellRate: String? = null,

    @SerializedName("DiamondClarity") val diamondClarity: String? = null,
    @SerializedName("DiamondColour") val diamondColour: String? = null,
    @SerializedName("DiamondShape") val diamondShape: String? = null,
    @SerializedName("DiamondCut") val diamondCut: String? = null,
    @SerializedName("DiamondSettingType") val diamondSettingType: String? = null,
    @SerializedName("DiamondCertificate") val diamondCertificate: String? = null,
    @SerializedName("DiamondPieces") val diamondPieces: String? = null,

    @SerializedName("DiamondPurchaseAmount") val diamondPurchaseAmount: String? = null,
    @SerializedName("DiamondSellAmount") val diamondSellAmount: String? = null,

    @SerializedName("DiamondDescription") val diamondDescription: String? = null,

    @SerializedName("CreatedOn") val createdOn: String? = null,
    @SerializedName("LastUpdated") val lastUpdated: String? = null,

    @SerializedName("NetAmount") val netAmount: String? = null,
    @SerializedName("GSTAmount") val gstAmount: String? = null,

    @SerializedName("TotalAmount") val totalAmount: String? = null,
    @SerializedName("Purity") val purity: String? = null,

    @SerializedName("CompanyId") val companyId: Int? = null,
    @SerializedName("BranchId") val branchId: Int? = null,
    @SerializedName("CounterId") val counterId: Int? = null,
    @SerializedName("EmployeeId") val employeeId: Int? = null,

    @SerializedName("LabelledStockId") val labelledStockId: Int? = null,

    @SerializedName("FineSilver") val fineSilver: String? = null,
    @SerializedName("FineGold") val fineGold: String? = null,
    @SerializedName("DebitSilver") val debitSilver: String? = null,
    @SerializedName("DebitGold") val debitGold: String? = null,
    @SerializedName("BalanceSilver") val balanceSilver: String? = null,
    @SerializedName("BalanceGold") val balanceGold: String? = null,

    @SerializedName("ConvertAmt") val convertAmt: String? = null,
    @SerializedName("PacketId") val packetId: Int? = null,

    // ⚠️ keys are lower-camel in JSON
    @SerializedName("finalPrice") val finalPrice: String? = null,
    @SerializedName("totalGstAmount") val totalGstAmount: String? = null,

    @SerializedName("StoneRateKarate") val stoneRateKarate: String? = null,
    @SerializedName("Quotationid") val quotationId: Int? = null,

    @SerializedName("MetalName") val metalName: String? = null,
    @SerializedName("Stones") val stones: Any? = null
)*/

/*data class Customer(
    @SerializedName("Id") val id: Int? = null,
    @SerializedName("FirstName") val firstName: String? = null,
    @SerializedName("LastName") val lastName: String? = null,
    @SerializedName("MiddleName") val middleName: String? = null,
    @SerializedName("Gender") val gender: String? = null,

    @SerializedName("DateOfBirth") val dateOfBirth: String? = null,

    @SerializedName("PerAddStreet") val perAddStreet: String? = null,
    @SerializedName("PerAddTown") val perAddTown: String? = null,
    @SerializedName("PerAddState") val perAddState: String? = null,
    @SerializedName("PerAddPincode") val perAddPincode: String? = null,

    @SerializedName("CurrAddStreet") val currAddStreet: String? = null,
    @SerializedName("CurrAddTown") val currAddTown: String? = null,
    @SerializedName("CurrAddPincode") val currAddPincode: String? = null,
    @SerializedName("CurrAddState") val currAddState: String? = null,

    @SerializedName("Area") val area: String? = null,
    @SerializedName("City") val city: String? = null,
    @SerializedName("Country") val country: String? = null,

    @SerializedName("Mobile") val mobile: String? = null,
    @SerializedName("Email") val email: String? = null,

    @SerializedName("Password") val password: String? = null,
    @SerializedName("CustomerLoginId") val customerLoginId: String? = null,

    @SerializedName("GstNo") val gstNo: String? = null,
    @SerializedName("PanNo") val panNo: String? = null,
    @SerializedName("AadharNo") val aadharNo: String? = null,

    @SerializedName("BalanceAmount") val balanceAmount: String? = null,
    @SerializedName("AdvanceAmount") val advanceAmount: String? = null,
    @SerializedName("Discount") val discount: String? = null,

    @SerializedName("CreditPeriod") val creditPeriod: String? = null,

    @SerializedName("FineGold") val fineGold: String? = null,
    @SerializedName("FineSilver") val fineSilver: String? = null,

    @SerializedName("ClientCode") val clientCode: String? = null,
    @SerializedName("VendorId") val vendorId: Int? = null,

    @SerializedName("AddToVendor") val addToVendor: Boolean? = null,

    @SerializedName("CustomerSlabId") val customerSlabId: Int? = null,
    @SerializedName("CreditPeriodId") val creditPeriodId: Int? = null,
    @SerializedName("RateOfInterestId") val rateOfInterestId: Int? = null,

    @SerializedName("Remark") val remark: String? = null,
    @SerializedName("BranchId") val branchId: Int? = null,

    @SerializedName("OnlineStatus") val onlineStatus: String? = null,
    @SerializedName("MaritalStatus") val maritalStatus: String? = null,
    @SerializedName("DateOfMarriage") val dateOfMarriage: String? = null,

    // structure not shown -> keep Any
    @SerializedName("Addresses") val addresses: List<Any> = emptyList(),

    @SerializedName("CreatedOn") val createdOn: String? = null,
    @SerializedName("LastUpdated") val lastUpdated: String? = null,

    @SerializedName("StatusType") val statusType: Boolean? = null)*/

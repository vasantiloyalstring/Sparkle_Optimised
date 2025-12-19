package com.loyalstring.rfid.data.model.sampleIn

import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.sampleOut.SampleCustomerDto

data class SampleInResponse(@SerializedName("ItemCode") val itemCode: String? = null,
                            @SerializedName("SKU") val sku: String? = null,
                            @SerializedName("SKUId") val skuId: Int? = null,

                            @SerializedName("CategoryId") val categoryId: Int? = null,
                            @SerializedName("ProductId") val productId: Int? = null,
                            @SerializedName("DesignId") val designId: Int? = null,
                            @SerializedName("PurityId") val purityId: Int? = null,

                            @SerializedName("Quantity") val quantity: Int? = null,
                            @SerializedName("GrossWt") val grossWt: String? = null,
                            @SerializedName("NetWt") val netWt: String? = null,
                            @SerializedName("TotalWt") val totalWt: String? = null,

                            @SerializedName("FinePercentage") val finePercentage: String? = null,
                            @SerializedName("WastegePercentage") val wastegePercentage: String? = null,
                            @SerializedName("StoneWeight") val stoneWeight: String? = null,
                            @SerializedName("DiamondWeight") val diamondWeight: String? = null,
                            @SerializedName("FineWastageWt") val fineWastageWt: String? = null,

                            @SerializedName("RatePerGram") val ratePerGram: String? = null,
                            @SerializedName("MetalAmount") val metalAmount: String? = null,
                            @SerializedName("Description") val description: String? = null,

                            @SerializedName("SampleStatus") val sampleStatus: String? = null,
                            @SerializedName("ClientCode") val clientCode: String? = null,

                            @SerializedName("StoneAmount") val stoneAmount: String? = null,
                            @SerializedName("SampleOutNo") val sampleOutNo: String? = null,
                            @SerializedName("DiamondAmount") val diamondAmount: String? = null,
                            @SerializedName("Pieces") val pieces: String? = null,

                            @SerializedName("CategoryName") val categoryName: String? = null,
                            @SerializedName("ProductName") val productName: String? = null,
                            @SerializedName("PurityName") val purityName: String? = null,
                            @SerializedName("DesignName") val designName: String? = null,

                            @SerializedName("Id") val id: Int? = null,
                            @SerializedName("CustomerId") val customerId: Int? = null,
                            @SerializedName("VendorId") val vendorId: Int? = null,

                            @SerializedName("CreatedOn") val createdOn: String? = null,
                            @SerializedName("CustomerName") val customerName: String? = null,
                            @SerializedName("SampleInDate") val sampleInDate: String? = null,

                            @SerializedName("BranchId") val branchId: Int? = null,

                            @SerializedName("Customer") val customer: SampleCustomerDto? = null,
                            @SerializedName("LabelledStockId") val labelledStockId: Int? = null)

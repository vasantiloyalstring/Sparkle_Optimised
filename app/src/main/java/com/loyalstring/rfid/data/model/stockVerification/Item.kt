package com.loyalstring.rfid.data.model.stockVerification

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Item( @SerializedName("ItemCode")
                 @Expose
                 val itemCode: String? = null,

                 @SerializedName("Status")
                 @Expose
                 val status: String? = null,

                 @SerializedName("GrossWeight")
                 @Expose
                 val grossWeight: Double = 0.0,

                 @SerializedName("NetWeight")
                 @Expose
                 val netWeight: Double = 0.0,

                 @SerializedName("Quantity")
                 @Expose
                 val quantity: Int? = null,

                 @SerializedName("CounterName")
                 @Expose
                 val counterName: String? = null,

                 @SerializedName("CategoryName")
                 @Expose
                 val categoryName: String? = null,

                 @SerializedName("ProductName")
                 @Expose
                 val productName: String? = null,

                 @SerializedName("DesignName")
                 @Expose
                 val designName: String? = null,

                 @SerializedName("PurityName")
                 @Expose
                 val purityName: String? = null,

                 @SerializedName("CompanyName")
                 @Expose
                 val companyName: String? = null,

                 @SerializedName("BranchName")
                 @Expose
                 val branchName: String? = null,

                 @SerializedName("CounterId")
                 @Expose
                 val counterId: Int? = null,

                 @SerializedName("CategoryId")
                 @Expose
                 val categoryId: Int? = null,

                 @SerializedName("ProductId")
                 @Expose
                 val productId: Int? = null,

                 @SerializedName("DesignId")
                 @Expose
                 val designId: Int? = null,

                 @SerializedName("PurityId")
                 @Expose
                 val purityId: Int? = null,

                 @SerializedName("CompanyId")
                 @Expose
                 val companyId: Int? = null,

                 @SerializedName("BranchId")
                 @Expose
                 val branchId: Int? = null)
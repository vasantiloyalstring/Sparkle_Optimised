package com.loyalstring.rfid.data.remote.response

import com.google.gson.annotations.SerializedName

data class ClearStockDataModelResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("deletedRecords")
    val deletedRecords: Int = 0
)

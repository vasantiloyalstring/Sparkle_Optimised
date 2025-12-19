package com.loyalstring.rfid.data.remote.data
import com.google.gson.annotations.SerializedName

data class ClearStockDataModelReq(
    @SerializedName("ClientCode")
    val clientCode: String = "",

    @SerializedName("DeviceId")
    val deviceId: String = ""
)

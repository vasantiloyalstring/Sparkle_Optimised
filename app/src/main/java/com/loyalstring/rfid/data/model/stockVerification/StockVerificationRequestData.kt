package com.loyalstring.rfid.data.model.stockVerification

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.stockVerification.Item

data class StockVerificationRequestData(  @SerializedName("ClientCode")
                                          @Expose
                                          val clientCode: String? = null,

                                          @SerializedName("Items")
                                          @Expose
                                          val items: List<Item>? = null)

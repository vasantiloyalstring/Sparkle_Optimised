package com.loyalstring.rfid.data.remote.response

data class BranchLabelledStocksResponse(  val TotalCount: Int = 0,
                                          val PageNumber: Int = 1,
                                          val PageSize: Int = 50,
                                          val Items: List<AlllabelResponse.LabelItem> = emptyList())

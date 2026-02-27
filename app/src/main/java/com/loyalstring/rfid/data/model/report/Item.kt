package com.loyalstring.rfid.data.model.report

data class Item(    val ItemCode: String?,
                    val RFIDCode: String?,
                    val TIDNumber: String?,
                    val Status: String?,
                    val GrossWeight: Double?,
                    val NetWeight: Double?,
                    val CategoryName: String?,
                    val ProductName: String?,
                    val DesignName: String?)

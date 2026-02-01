package com.loyalstring.rfid.data.model.login

data class SyncSkippedItem(
    val itemCode: String,
    val rfid: String?,
    val tid: String?,
    val reason: String
)

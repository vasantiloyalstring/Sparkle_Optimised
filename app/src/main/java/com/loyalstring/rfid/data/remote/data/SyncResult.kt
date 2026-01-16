package com.loyalstring.rfid.data.remote.data

data class SyncResult( val total: Int,
                       val synced: Int,
                       val skipped: List<String>)

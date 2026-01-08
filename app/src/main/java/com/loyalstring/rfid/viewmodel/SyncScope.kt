package com.loyalstring.rfid.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScope @Inject constructor() {
    val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
}

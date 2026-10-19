package com.loyalstring.rfid.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NormalRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JewelMartsRetrofit

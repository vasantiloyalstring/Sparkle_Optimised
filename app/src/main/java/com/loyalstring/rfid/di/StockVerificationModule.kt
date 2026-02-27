package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.StockVerificationRepository
import com.loyalstring.rfid.repository.StockVerificationRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StockVerificationModule {

    @Provides
    @Singleton
    fun provideStockVerificationRepository(
        apiService: RetrofitInterface
    ): StockVerificationRepository {
        return StockVerificationRepositoryImpl(apiService)
    }
}
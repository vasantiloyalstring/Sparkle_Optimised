package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.DeliveryChallanRepository
import com.loyalstring.rfid.repository.DeliveryChallanRepositoryImpl
import com.loyalstring.rfid.repository.QuotationRepository
import com.loyalstring.rfid.repository.QuotationRepositoryImple
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
class QuotationModule {
    @Provides
    @Singleton
    fun provideQuotationRepository(
        apiService: RetrofitInterface
    ): QuotationRepository {
        return QuotationRepositoryImple(apiService)
    }
}
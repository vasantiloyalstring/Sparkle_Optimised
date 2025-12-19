package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.SampleInRepository
import com.loyalstring.rfid.repository.SampleInRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SampleInModule {
    @Provides
    @Singleton
    fun provideSampleInRepository(
        apiService: RetrofitInterface
    ): SampleInRepository {
        return SampleInRepositoryImpl(apiService)
    }
}
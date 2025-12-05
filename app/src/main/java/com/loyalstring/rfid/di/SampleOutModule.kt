package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.SampleOutRepositoryImpl
import com.loyalstring.rfid.repository.SampleOutRepositoty
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SampleOutModule {

    @Provides
    @Singleton
    fun provideSampleoutRepository(
        apiService: RetrofitInterface
    ): SampleOutRepositoty {
        return SampleOutRepositoryImpl(apiService)
    }
}
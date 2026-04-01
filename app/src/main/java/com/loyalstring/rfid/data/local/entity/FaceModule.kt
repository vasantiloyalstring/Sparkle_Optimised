package com.loyalstring.rfid.data.local.entity

import com.loyalstring.rfid.data.local.dao.FaceDao
import com.loyalstring.rfid.repository.FaceRepository
import com.loyalstring.rfid.repository.FaceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object FaceModule {

    @Provides
    fun provideFaceRepository(faceDao: FaceDao): FaceRepository {
        return FaceRepositoryImpl(faceDao)
    }
}
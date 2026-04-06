package com.loyalstring.rfid.data.local.entity

import com.loyalstring.rfid.data.local.dao.FaceDao
import com.loyalstring.rfid.data.local.db.AppDatabase
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.FaceRepository
import com.loyalstring.rfid.repository.FaceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FaceModule {

    @Provides
    @Singleton
    fun provideFaceDao(appDatabase: AppDatabase): FaceDao {
        return appDatabase.faceDao()
    }

 /*   @Provides
    @Singleton
    fun provideFaceRepository(faceDao: FaceDao): FaceRepository {
        return FaceRepositoryImpl(faceDao)
    }*/

    @Provides
    @Singleton
    fun provideFaceRepository(retrofitInterface: RetrofitInterface): FaceRepository {
        return FaceRepositoryImpl(retrofitInterface)
    }
}
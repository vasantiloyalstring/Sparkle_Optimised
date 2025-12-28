package com.loyalstring.rfid.data.remote.network

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.di.NormalRetrofit
import com.loyalstring.rfid.di.SyncRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetWorkRetrofitClient {

    @Provides
    fun provideBaseUrl() = "https://rrgold.loyalstring.co.in/"

    /* ---------------- NORMAL OKHTTP ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    /* ---------------- SYNC OKHTTP (STREAMING) ---------------- */

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    /* ---------------- NORMAL RETROFIT ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalRetrofit(
        @NormalRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

    /* ---------------- SYNC RETROFIT (NO CONVERTERS) ---------------- */

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncRetrofit(
        @SyncRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    /* ---------------- API SERVICES ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalApi(
        @NormalRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncApi(
        @SyncRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    /* ---------------- DEFAULT API (CRITICAL FIX) ---------------- */

    @Provides
    @Singleton
    fun provideDefaultApi(
        @NormalRetrofit api: RetrofitInterface
    ): RetrofitInterface = api
}

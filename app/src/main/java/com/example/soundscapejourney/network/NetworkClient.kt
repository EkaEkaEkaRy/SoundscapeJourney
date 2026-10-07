package com.example.soundscapejourney.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.soundscapejourney.BuildConfig


object NetworkClient {
    private const val BASE_URL = BuildConfig.FREESOUND_URL

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val freesoundApi: FreesoundApi by lazy {
        retrofit.create(FreesoundApi::class.java)
    }
}

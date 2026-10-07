package com.example.soundscapejourney.network

import com.example.soundscapejourney.data.models.FreesoundResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FreesoundApi {

    // Делаем GET запрос на эндпоинт поиска звуков
    @GET("apiv2/search/text/")
    suspend fun searchSounds(
        @Query("query") query: String,
        @Query("token") token: String,
        @Query("fields") fields: String = "id,name,username,duration,tags,images,previews"
    ): FreesoundResponse
}
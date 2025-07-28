package com.kevinluis.newsapp.model.remote.retrofit

import com.kevinluis.newsapp.model.remote.response.NewsResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("top-headlines")
    fun getTopHeadlines(
        @Query("country") country: String
    ): Call<NewsResponse>

    @GET("everything")
    fun getEverything(
        @Query("q") query: String,
        @Query("language") language: String,
        @Query("excludeDomain") excludeDomain: String,
        @Query("sortBy") sortBy: String
    ): Call<NewsResponse>


}
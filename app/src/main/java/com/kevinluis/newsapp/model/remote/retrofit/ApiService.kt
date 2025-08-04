package com.kevinluis.newsapp.model.remote.retrofit

import com.kevinluis.newsapp.model.remote.response.NewsResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("top-headlines?country=us")
    fun getTopHeadlines(
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>

    @GET("everything?q=indonesia&language=id&sortBy=publishedAt")
    fun getEverything(
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}
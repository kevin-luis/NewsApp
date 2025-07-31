package com.kevinluis.newsapp.di

import android.content.Context
import com.kevinluis.newsapp.model.local.room.NewsDatabase
import com.kevinluis.newsapp.model.remote.retrofit.ApiConfig
import com.kevinluis.newsapp.model.repository.NewsRepository
import com.kevinluis.newsapp.viewmodel.utils.AppExecutors

object Injection {
    fun provideRepository(context: Context): NewsRepository {
        val apiService = ApiConfig.getApiService()
        val database = NewsDatabase.getInstance(context)
        val dao = database.newsDao()
        val appExecutors = AppExecutors()
        return NewsRepository.getInstance(apiService, dao, appExecutors)
    }
}
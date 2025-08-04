package com.kevinluis.newsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kevinluis.newsapp.model.Result
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.model.repository.NewsRepository
import android.util.Log

class NewsViewModel(private val newsRepository: NewsRepository) : ViewModel() {

    companion object {
        private const val TAG = "NewsViewModel"
    }

    // Single source of truth untuk LiveData
    fun getHeadlineNews(): LiveData<Result<List<NewsEntity>>> {
        Log.d(TAG, "getHeadlineNews called")
        return newsRepository.getHeadlineNews()
    }

    fun getAllNewsAlways(): LiveData<Result<List<ArticlesItem>>> {
        Log.d(TAG, "getAllNewsAlways called")
        return newsRepository.getAllNewsAlways()
    }

    // Methods untuk trigger data load
    fun triggerDataLoad() {
        Log.d(TAG, "Triggering data load")
        newsRepository.triggerHeadlineNewsLoad()
        newsRepository.triggerAllNewsLoad()
    }

    // Cache management methods
    fun getCachedHeadlineNews(): List<NewsEntity>? {
        val cached = newsRepository.getCachedHeadlineNews()
        Log.d(TAG, "getCachedHeadlineNews: ${cached?.size ?: 0} items")
        return cached
    }

    fun getCachedAllNews(): List<ArticlesItem>? {
        val cached = newsRepository.getCachedAllNews()
        Log.d(TAG, "getCachedAllNews: ${cached?.size ?: 0} items")
        return cached
    }

    fun isHeadlineNewsCacheValid(): Boolean {
        val isValid = newsRepository.isHeadlineNewsCacheValid()
        Log.d(TAG, "isHeadlineNewsCacheValid: $isValid")
        return isValid
    }

    fun isAllNewsCacheValid(): Boolean {
        val isValid = newsRepository.isAllNewsCacheValid()
        Log.d(TAG, "isAllNewsCacheValid: $isValid")
        return isValid
    }

    // Enhanced Bookmark methods
    fun getBookmarkedNews(): LiveData<List<NewsEntity>> {
        Log.d(TAG, "getBookmarkedNews called")
        return newsRepository.getBookmarkedNews()
    }

    fun setBookmarkedNews(news: NewsEntity, bookmarkState: Boolean) {
        Log.d(TAG, "setBookmarkedNews: ${news.title}, bookmarked: $bookmarkState")
        newsRepository.setBookmarkedNews(news, bookmarkState)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
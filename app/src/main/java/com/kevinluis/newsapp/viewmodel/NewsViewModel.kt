package com.kevinluis.newsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kevinluis.newsapp.model.Result
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.model.repository.NewsRepository

class NewsViewModel(private val newsRepository: NewsRepository) : ViewModel() {
    fun getHeadlineNews() = newsRepository.getHeadlineNews()
    fun getAllNews() = newsRepository.getAllNews()
    fun getAllNewsAlways() = newsRepository.getAllNewsAlways()
    fun getBookmarkedNews() = newsRepository.getBookmarkedNews()
    fun getCachedAllNews() = newsRepository.getCachedAllNews()

    // ✅ TAMBAHKAN: Methods untuk HeadlineNews cache
    fun getCachedHeadlineNews() = newsRepository.getCachedHeadlineNews()
    fun isHeadlineNewsCacheValid() = newsRepository.isHeadlineNewsCacheValid()
    fun refreshHeadlineNews() = newsRepository.refreshHeadlineNews()
    fun clearHeadlineNewsCache() = newsRepository.clearHeadlineNewsCache()

    // ✅ TAMBAHKAN: Method untuk refresh semua data sekaligus
    fun refreshAllData() = newsRepository.refreshAllData()

    fun setBookmarkedNews(news: NewsEntity, bookmarkState: Boolean) {
        newsRepository.setBookmarkedNews(news, bookmarkState)
    }

    fun refreshAllNews() = newsRepository.refreshAllNews()
}
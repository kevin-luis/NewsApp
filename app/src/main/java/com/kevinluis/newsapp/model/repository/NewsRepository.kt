package com.kevinluis.newsapp.model.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kevinluis.newsapp.BuildConfig
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.local.room.NewsDao
import com.kevinluis.newsapp.model.remote.response.NewsResponse
import com.kevinluis.newsapp.model.remote.retrofit.ApiService
import com.kevinluis.newsapp.viewmodel.utils.AppExecutors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import com.kevinluis.newsapp.model.Result
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import android.util.Log

class NewsRepository private constructor(
    private val apiService: ApiService,
    private val newsDao: NewsDao,
    private val appExecutors: AppExecutors
) {

    private val headlineNewsLiveData = MutableLiveData<Result<List<NewsEntity>>>()
    private val allNewsLiveData = MutableLiveData<Result<List<ArticlesItem>>>()

    // Cache untuk getAllNews
    private var allNewsCache: List<ArticlesItem>? = null
    private var allNewsCacheTime: Long = 0
    private var isAllNewsLoading = false

    // Cache untuk HeadlineNews
    private var headlineNewsCache: List<NewsEntity>? = null
    private var headlineNewsCacheTime: Long = 0
    private var isHeadlineNewsLoading = false

    // Cache duration (5 menit untuk lebih responsive)
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes in milliseconds

    companion object {
        private const val TAG = "NewsRepository"

        @Volatile
        private var instance: NewsRepository? = null
        fun getInstance(
            apiService: ApiService,
            newsDao: NewsDao,
            appExecutors: AppExecutors
        ): NewsRepository =
            instance ?: synchronized(this) {
                instance ?: NewsRepository(apiService, newsDao, appExecutors)
            }.also { instance = it }
    }
    fun getHeadlineNews(): LiveData<Result<List<NewsEntity>>> {
        return headlineNewsLiveData
    }

    fun getAllNewsAlways(): LiveData<Result<List<ArticlesItem>>> {
        return allNewsLiveData
    }

    // Method untuk trigger data load manual
    fun triggerHeadlineNewsLoad() {
        val currentTime = System.currentTimeMillis()

        // Jika cache masih valid, langsung return cached data
        if (headlineNewsCache != null && (currentTime - headlineNewsCacheTime) < CACHE_DURATION) {
            Log.d(TAG, "Using cached headline news")
            headlineNewsLiveData.value = Result.Success(headlineNewsCache!!)
            return
        }

        // Jika sedang loading, jangan trigger lagi
        if (isHeadlineNewsLoading) {
            Log.d(TAG, "Headline news already loading")
            return
        }

        // Fetch fresh data
        fetchHeadlineNewsFromApi()
    }

    fun triggerAllNewsLoad() {
        val currentTime = System.currentTimeMillis()

        // Jika cache masih valid, langsung return cached data
        if (allNewsCache != null && (currentTime - allNewsCacheTime) < CACHE_DURATION) {
            Log.d(TAG, "Using cached all news")
            allNewsLiveData.value = Result.Success(allNewsCache!!)
            return
        }

        // Jika sedang loading, jangan trigger lagi
        if (isAllNewsLoading) {
            Log.d(TAG, "All news already loading")
            return
        }

        // Fetch fresh data
        fetchAllNewsFromApi()
    }

    // Optimized fetch methods
    private fun fetchHeadlineNewsFromApi() {
        isHeadlineNewsLoading = true
        headlineNewsLiveData.value = Result.Loading

        Log.d(TAG, "Fetching headline news from API")

        val client = apiService.getTopHeadlines(BuildConfig.API_KEY)
        client.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                isHeadlineNewsLoading = false
                if (response.isSuccessful) {
                    val articles = response.body()?.articles
                    if (articles != null && articles.isNotEmpty()) {
                        val newsList = ArrayList<NewsEntity>()
                        appExecutors.diskIO.execute {
                            articles.forEach { article ->
                                val isBookmarked = newsDao.isNewsBookmarked(article.title)

                                val news = NewsEntity(
                                    title = article.title,
                                    publishedAt = article.publishedAt,
                                    urlToImage = article.urlToImage,
                                    url = article.url,
                                    sourceName = article.source.name,
                                    author = article.author,
                                    description = article.description,
                                    content = article.content,
                                    isBookmarked = isBookmarked
                                )
                                newsList.add(news)
                            }

                            // Update database
                            newsDao.deleteAll()
                            newsDao.insertNews(newsList)

                            // Update cache
                            headlineNewsCache = newsList
                            headlineNewsCacheTime = System.currentTimeMillis()

                            // Notify observers
                            headlineNewsLiveData.postValue(Result.Success(newsList))

                            Log.d(TAG, "Headline news cached: ${newsList.size} items")
                        }
                    } else {
                        headlineNewsLiveData.value = Result.Error("No headline articles found")
                    }
                } else {
                    headlineNewsLiveData.value = Result.Error("Failed to load headline news: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse?>, t: Throwable) {
                isHeadlineNewsLoading = false
                Log.e(TAG, "Headline news API call failed", t)

                // Jika ada cached data, gunakan itu sebagai fallback
                if (headlineNewsCache != null) {
                    Log.d(TAG, "Using cached headline news as fallback")
                    headlineNewsLiveData.value = Result.Success(headlineNewsCache!!)
                } else {
                    headlineNewsLiveData.value = Result.Error(t.message ?: "Network error occurred")
                }
            }
        })
    }

    private fun fetchAllNewsFromApi() {
        isAllNewsLoading = true
        allNewsLiveData.value = Result.Loading

        Log.d(TAG, "Fetching all news from API")

        val client = apiService.getEverything(BuildConfig.API_KEY)
        client.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                isAllNewsLoading = false
                if (response.isSuccessful) {
                    val articles = response.body()?.articles
                    if (articles != null && articles.isNotEmpty()) {
                        // Update cache
                        allNewsCache = articles
                        allNewsCacheTime = System.currentTimeMillis()

                        // Notify observers
                        allNewsLiveData.value = Result.Success(articles)

                        Log.d(TAG, "All news cached: ${articles.size} items")
                    } else {
                        allNewsLiveData.value = Result.Error("No articles found")
                    }
                } else {
                    allNewsLiveData.value = Result.Error("Response not successful: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                isAllNewsLoading = false
                Log.e(TAG, "All news API call failed", t)

                // Fallback to cache if available
                if (allNewsCache != null) {
                    Log.d(TAG, "Using cached all news as fallback")
                    allNewsLiveData.value = Result.Success(allNewsCache!!)
                } else {
                    allNewsLiveData.value = Result.Error(t.message ?: "Network error occurred")
                }
            }
        })
    }

    // Simplified cache getters
    fun getCachedHeadlineNews(): List<NewsEntity>? {
        return if (isHeadlineNewsCacheValid()) {
            Log.d(TAG, "Returning cached headline news: ${headlineNewsCache?.size} items")
            headlineNewsCache
        } else {
            Log.d(TAG, "Headline news cache is invalid or empty")
            null
        }
    }

    fun getCachedAllNews(): List<ArticlesItem>? {
        return if (isAllNewsCacheValid()) {
            Log.d(TAG, "Returning cached all news: ${allNewsCache?.size} items")
            allNewsCache
        } else {
            Log.d(TAG, "All news cache is invalid or empty")
            null
        }
    }

    fun isHeadlineNewsCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        val isValid = headlineNewsCache != null && (currentTime - headlineNewsCacheTime) < CACHE_DURATION
        Log.d(TAG, "Headline news cache valid: $isValid")
        return isValid
    }

    fun isAllNewsCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        val isValid = allNewsCache != null && (currentTime - allNewsCacheTime) < CACHE_DURATION
        Log.d(TAG, "All news cache valid: $isValid")
        return isValid
    }

    // Enhanced Bookmarks methods
    fun getBookmarkedNews(): LiveData<List<NewsEntity>> {
        return newsDao.getBookmarkedNews()
    }

    fun setBookmarkedNews(news: NewsEntity, bookmarkState: Boolean) {
        Log.d(TAG, "setBookmarkedNews called: title=${news.title}, state=$bookmarkState")

        appExecutors.diskIO.execute {
            try {
                // Update atau insert NewsEntity ke database
                news.isBookmarked = bookmarkState

                if (bookmarkState) {
                    // Jika bookmark, pastikan data tersimpan di database
                    Log.d(TAG, "Adding bookmark: ${news.title}")
                    newsDao.insertBookmarkNews(news)
                } else {
                    // Jika unbookmark, update status
                    Log.d(TAG, "Removing bookmark: ${news.title}")
                    newsDao.updateNews(news)
                }

                // Update cache if exists
                headlineNewsCache?.find { it.title == news.title }?.let {
                    it.isBookmarked = bookmarkState
                    Log.d(TAG, "Updated headline cache bookmark status for: ${news.title}")
                }

                Log.d(TAG, "Bookmark operation completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error in setBookmarkedNews", e)
            }
        }
    }
}
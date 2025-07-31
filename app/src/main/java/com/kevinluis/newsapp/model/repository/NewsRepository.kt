package com.kevinluis.newsapp.model.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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

class NewsRepository private constructor(
    private val apiService: ApiService,
    private val newsDao: NewsDao,
    private val appExecutors: AppExecutors
) {

    private val result = MediatorLiveData<Result<List<NewsEntity>>>()

    // Cache untuk getAllNews
    private var allNewsCache: List<ArticlesItem>? = null
    private var allNewsCacheTime: Long = 0
    private val allNewsResult = MutableLiveData<Result<List<ArticlesItem>>>()
    private var isAllNewsLoading = false

    // ✅ TAMBAHKAN: Cache untuk HeadlineNews
    private var headlineNewsCache: List<NewsEntity>? = null
    private var headlineNewsCacheTime: Long = 0
    private val headlineNewsResult = MutableLiveData<Result<List<NewsEntity>>>()
    private var isHeadlineNewsLoading = false

    // Cache duration (10 menit untuk news)
    private val CACHE_DURATION = 10 * 60 * 1000L // 10 minutes in milliseconds

    // ✅ REFACTOR: getHeadlineNews dengan caching
    fun getHeadlineNews(): LiveData<Result<List<NewsEntity>>> {
        val currentTime = System.currentTimeMillis()

        // Cek apakah cache masih valid
        if (headlineNewsCache != null && (currentTime - headlineNewsCacheTime) < CACHE_DURATION) {
            // INSTANT: Langsung return cached data
            headlineNewsResult.value = Result.Success(headlineNewsCache!!)
            return headlineNewsResult
        }

        // Cek apakah sedang loading untuk mencegah duplicate request
        if (isHeadlineNewsLoading) {
            return headlineNewsResult
        }

        // Fetch data baru jika cache expired atau tidak ada
        fetchHeadlineNewsFromApi()
        return headlineNewsResult
    }

    // ✅ TAMBAHKAN: Method terpisah untuk fetch API
    private fun fetchHeadlineNewsFromApi() {
        isHeadlineNewsLoading = true
        headlineNewsResult.value = Result.Loading

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
                                    article.title,
                                    article.publishedAt,
                                    article.urlToImage,
                                    article.url,
                                    article.source.name,
                                    isBookmarked
                                )
                                newsList.add(news)
                            }
                            newsDao.deleteAll()
                            newsDao.insertNews(newsList)

                            // ✅ UPDATE: Cache setelah berhasil
                            headlineNewsCache = newsList
                            headlineNewsCacheTime = System.currentTimeMillis()
                            headlineNewsResult.postValue(Result.Success(newsList))
                        }
                    } else {
                        headlineNewsResult.value = Result.Error("No headline articles found")
                    }
                } else {
                    headlineNewsResult.value = Result.Error("Failed to load headline news: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse?>, t: Throwable) {
                isHeadlineNewsLoading = false
                // Jika ada cached data, gunakan itu sebagai fallback
                if (headlineNewsCache != null) {
                    headlineNewsResult.value = Result.Success(headlineNewsCache!!)
                } else {
                    headlineNewsResult.value = Result.Error(t.message ?: "Network error occurred")
                }
            }
        })

        // ✅ TETAP: Observe dari database untuk data real-time
        val localData = newsDao.getNews()
        result.addSource(localData) { newData: List<NewsEntity> ->
            if (newData.isNotEmpty()) {
                result.value = Result.Success(newData)
                // Update cache juga
                headlineNewsCache = newData
                headlineNewsCacheTime = System.currentTimeMillis()
            }
        }
    }

    // ✅ TAMBAHKAN: Method untuk get cached headline news
    fun getCachedHeadlineNews(): List<NewsEntity>? {
        return if (isHeadlineNewsCacheValid()) headlineNewsCache else null
    }

    // ✅ TAMBAHKAN: Method untuk cek validitas cache headline news
    fun isHeadlineNewsCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return headlineNewsCache != null && (currentTime - headlineNewsCacheTime) < CACHE_DURATION
    }

    // ✅ TAMBAHKAN: Method untuk refresh headline news
    fun refreshHeadlineNews(): LiveData<Result<List<NewsEntity>>> {
        clearHeadlineNewsCache()
        return getHeadlineNews()
    }

    // ✅ TAMBAHKAN: Method untuk clear cache headline news
    fun clearHeadlineNewsCache() {
        headlineNewsCache = null
        headlineNewsCacheTime = 0
        isHeadlineNewsLoading = false
    }

    // Method untuk force refresh semua data
    fun refreshAllData(): Pair<LiveData<Result<List<ArticlesItem>>>, LiveData<Result<List<NewsEntity>>>> {
        clearAllNewsCache()
        clearHeadlineNewsCache()
        return Pair(getAllNewsAlways(), getHeadlineNews())
    }

    // ... kode lainnya tetap sama
    fun getAllNews(): LiveData<Result<List<ArticlesItem>>> {
        val currentTime = System.currentTimeMillis()

        if (allNewsCache != null && (currentTime - allNewsCacheTime) < CACHE_DURATION) {
            allNewsResult.value = Result.Success(allNewsCache!!)
            return allNewsResult
        }

        if (isAllNewsLoading) {
            return allNewsResult
        }

        fetchAllNewsFromApi()
        return allNewsResult
    }

    private fun fetchAllNewsFromApi() {
        isAllNewsLoading = true
        allNewsResult.value = Result.Loading

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
                        allNewsCache = articles
                        allNewsCacheTime = System.currentTimeMillis()
                        allNewsResult.value = Result.Success(articles)
                    } else {
                        allNewsResult.value = Result.Error("No articles found")
                    }
                } else {
                    allNewsResult.value = Result.Error("Response not successful: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                isAllNewsLoading = false
                if (allNewsCache != null) {
                    allNewsResult.value = Result.Success(allNewsCache!!)
                } else {
                    allNewsResult.value = Result.Error(t.message ?: "Network error occurred")
                }
            }
        })
    }

    fun getAllNewsAlways(): LiveData<Result<List<ArticlesItem>>> {
        val freshResult = MutableLiveData<Result<List<ArticlesItem>>>()
        val currentTime = System.currentTimeMillis()

        if (allNewsCache != null) {
            freshResult.value = Result.Success(allNewsCache!!)

            if ((currentTime - allNewsCacheTime) < CACHE_DURATION) {
                return freshResult
            }
        }

        if (!isAllNewsLoading) {
            isAllNewsLoading = true
            if (allNewsCache == null) {
                freshResult.value = Result.Loading
            }

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
                            allNewsCache = articles
                            allNewsCacheTime = System.currentTimeMillis()
                            freshResult.value = Result.Success(articles)
                            allNewsResult.value = Result.Success(articles)
                        } else {
                            freshResult.value = Result.Error("No articles found")
                        }
                    } else {
                        freshResult.value = Result.Error("Response not successful: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                    isAllNewsLoading = false
                    if (allNewsCache == null) {
                        freshResult.value = Result.Error(t.message ?: "Network error occurred")
                    }
                }
            })
        }

        return freshResult
    }

    fun getBookmarkedNews(): LiveData<List<NewsEntity>> {
        return newsDao.getBookmarkedNews()
    }

    fun setBookmarkedNews(news: NewsEntity, bookmarkState: Boolean) {
        appExecutors.diskIO.execute {
            news.isBookmarked = bookmarkState
            newsDao.updateNews(news)
        }
    }

    fun refreshAllNews(): LiveData<Result<List<ArticlesItem>>> {
        clearAllNewsCache()
        return getAllNewsAlways()
    }

    fun clearAllNewsCache() {
        allNewsCache = null
        allNewsCacheTime = 0
        isAllNewsLoading = false
    }

    fun isCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return allNewsCache != null && (currentTime - allNewsCacheTime) < CACHE_DURATION
    }

    fun getCacheAgeInMinutes(): Long {
        if (allNewsCache == null) return -1
        val currentTime = System.currentTimeMillis()
        return (currentTime - allNewsCacheTime) / (60 * 1000)
    }

    fun getCachedAllNews(): List<ArticlesItem>? {
        return if (isCacheValid()) allNewsCache else null
    }

    companion object {
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
}
package com.kevinluis.newsapp.model.local.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kevinluis.newsapp.model.local.entity.NewsEntity

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY publishedAt DESC")
    fun getNews(): LiveData<List<NewsEntity>>

    @Query("SELECT * FROM news where isBookmarked = 1 ORDER BY publishedAt DESC")
    fun getBookmarkedNews(): LiveData<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNews(news: List<NewsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookmarkNews(news: NewsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateNews(news: NewsEntity)

    @Update
    fun updateNews(news: NewsEntity)

    @Query("UPDATE news SET isBookmarked = :isBookmarked WHERE title = :title")
    fun updateBookmarkStatus(title: String, isBookmarked: Boolean)

    @Query("DELETE FROM news WHERE isBookmarked = 0")
    fun deleteAll()

    @Query("SELECT EXISTS(SELECT * FROM news WHERE title = :title AND isBookmarked = 1)")
    fun isNewsBookmarked(title: String): Boolean

    @Query("SELECT * FROM news WHERE title = :title LIMIT 1")
    fun getNewsByTitle(title: String): NewsEntity?

    @Query("SELECT * FROM news WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun getBookmarkedNewsList(): List<NewsEntity>
}
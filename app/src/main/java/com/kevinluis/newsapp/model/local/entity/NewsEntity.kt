package com.kevinluis.newsapp.model.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @field:PrimaryKey
    @field:ColumnInfo(name = "title")
    val title: String,

    @field:ColumnInfo(name = "publishedAt")
    val publishedAt: String?,

    @field:ColumnInfo(name = "urlToImage")
    val urlToImage: String? = null,

    @field:ColumnInfo(name = "url")
    val url: String? = null,

    @field:ColumnInfo(name = "sourceName")
    val sourceName: String? = null,

    @field:ColumnInfo(name = "author")
    val author: String? = null,

    @field:ColumnInfo(name = "description")
    val description: String? = null,

    @field:ColumnInfo(name = "content")
    val content: String? = null,

    @field:ColumnInfo(name = "isBookmarked")
    var isBookmarked: Boolean = false
)
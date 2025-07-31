package com.kevinluis.newsapp.model.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
class NewsEntity (
    @field:ColumnInfo(name="title")
    @field:PrimaryKey
    val title: String,

    @field:ColumnInfo(name="publishedAt")
    val publishedAt: String,

    @field:ColumnInfo(name="urlToImage")
    val urlToImage: String? = null,

    @field:ColumnInfo(name="url")
    val url: String? = null,

    @field:ColumnInfo(name="source")
    val source: String? = null,

    @field:ColumnInfo(name="isBookmarked")
    var isBookmarked: Boolean

)
package com.kevinluis.newsapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.ItemHeadlineNewsBinding
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class HeadlineNewsAdapter : ListAdapter<NewsEntity, HeadlineNewsAdapter.HeadlineNewsViewHolder>(DIFF_CALLBACK){

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HeadlineNewsViewHolder {
        val binding = ItemHeadlineNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeadlineNewsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HeadlineNewsViewHolder,
        position: Int
    ) {
        val headlineNews = getItem(position)
        holder.bind(headlineNews)
    }

    class HeadlineNewsViewHolder(val binding: ItemHeadlineNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(news: NewsEntity) {
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .override(500)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.broken_image)
                .into(binding.imageViewBackground)

            binding.textViewTitle.text = news.title ?: "Judul tidak tersedia"
            binding.textViewSource.text = news.source ?: "Sumber tidak diketahui"
            binding.textViewDate.text = news.publishedAt?.let { DateConverter.convertToIndonesianDateModern(it) } ?: "Tanggal tidak tersedia"
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NewsEntity> () {
            override fun areItemsTheSame(
                oldItem: NewsEntity,
                newItem: NewsEntity
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: NewsEntity,
                newItem: NewsEntity
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}
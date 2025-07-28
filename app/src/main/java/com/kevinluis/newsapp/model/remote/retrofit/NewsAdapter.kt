package com.kevinluis.newsapp.model.remote.retrofit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.ItemNewsBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class NewsAdapter : ListAdapter<ArticlesItem, NewsAdapter.NewsViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = getItem(position)
        holder.bind(news)
    }

    class NewsViewHolder(val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(news: ArticlesItem) {
            // Load image dengan fallback
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .override(300)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder) // Tambahkan placeholder
                .error(R.drawable.broken_image) // Tambahkan error image
                .into(binding.ivNews)

            // Safe text binding
            binding.tvTitle.text = news.title ?: "Judul tidak tersedia"
            binding.tvDate.text = news.publishedAt?.let {
                DateConverter.convertToIndonesianDateModern(it)
            } ?: "Tanggal tidak tersedia"
            binding.tvSource.text = news.source?.name ?: "Sumber tidak diketahui"
            binding.tvAuthor.text = news.author ?: "Penulis tidak diketahui"
        }

    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticlesItem> () {
            override fun areItemsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                return  oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
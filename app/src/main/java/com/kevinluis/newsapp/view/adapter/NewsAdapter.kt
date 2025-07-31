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
import com.kevinluis.newsapp.databinding.ItemNewsBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class NewsAdapter(
    private val onItemClick: ((ArticlesItem) -> Unit)? = null
) : ListAdapter<ArticlesItem, NewsAdapter.NewsViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = getItem(position)
        if (news != null) {
            holder.bind(news)
        }
    }

    class NewsViewHolder(
        private val binding: ItemNewsBinding,
        private val onItemClick: ((ArticlesItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: ArticlesItem) {
            // Set click listener
            binding.root.setOnClickListener {
                onItemClick?.invoke(news)
            }

            // Load image dengan error handling yang lebih baik
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .override(400, 250) // Ukuran yang lebih proporsional
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.broken_image)
                .into(binding.ivNews)

            // Safe text binding dengan null safety
            binding.tvTitle.text = news.title?.takeIf { it.isNotBlank() }
                ?: "Judul tidak tersedia"

            binding.tvDate.text = try {
                news.publishedAt?.let { publishedAt ->
                    DateConverter.convertToIndonesianDateModern(publishedAt)
                } ?: "Tanggal tidak tersedia"
            } catch (e: Exception) {
                "Tanggal tidak valid"
            }

            binding.tvSource.text = news.source?.name?.takeIf { it.isNotBlank() }
                ?: "Sumber tidak diketahui"

            binding.tvAuthor.text = news.author?.takeIf { it.isNotBlank() }
                ?: "Penulis tidak diketahui"
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticlesItem>() {
            override fun areItemsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                // Gunakan URL sebagai identifier unik karena ArticlesItem mungkin tidak punya ID
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                return oldItem.title == newItem.title &&
                        oldItem.publishedAt == newItem.publishedAt &&
                        oldItem.urlToImage == newItem.urlToImage &&
                        oldItem.source?.name == newItem.source?.name &&
                        oldItem.author == newItem.author
            }
        }
    }
}
package com.kevinluis.newsapp.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.ItemHeadlineNewsBinding
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class HeadlineNewsAdapter(
    private val onItemClick: ((NewsEntity) -> Unit)? = null
) : ListAdapter<NewsEntity, HeadlineNewsAdapter.HeadlineNewsViewHolder>(DIFF_CALLBACK) {

    // ✅ OPTIMASI 1: RequestOptions yang di-reuse
    private val glideOptions = RequestOptions()
        .override(500, 300)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.image_placeholder)
        .error(R.drawable.broken_image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeadlineNewsViewHolder {
        val binding = ItemHeadlineNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeadlineNewsViewHolder(binding, onItemClick, glideOptions)
    }

    override fun onBindViewHolder(holder: HeadlineNewsViewHolder, position: Int) {
        val headlineNews = getItem(position)
        holder.bind(headlineNews)
    }

    // ✅ OPTIMASI 2: ViewHolder dengan optimasi binding
    class HeadlineNewsViewHolder(
        private val binding: ItemHeadlineNewsBinding,
        private val onItemClick: ((NewsEntity) -> Unit)?,
        private val glideOptions: RequestOptions
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: NewsEntity) {
            // ✅ OPTIMASI 3: Set click listener dengan data lengkap
            binding.root.setOnClickListener {
                onItemClick?.invoke(news)
            }

            // ✅ OPTIMASI 4: Load image dengan pre-configured options
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .apply(glideOptions)
                .into(binding.imageViewBackground)

            // ✅ OPTIMASI 5: Safe text binding dengan cache-friendly approach
            binding.textViewTitle.text = news.title ?: "Judul tidak tersedia"
            binding.textViewSource.text = news.sourceName ?: "Sumber tidak diketahui"

            // ✅ OPTIMASI 6: Date conversion dengan try-catch untuk performa
            binding.textViewDate.text = try {
                news.publishedAt?.let { DateConverter.convertToIndonesianDateModern(it) }
                    ?: "Tanggal tidak tersedia"
            } catch (e: Exception) {
                "Tanggal tidak tersedia"
            }
        }
    }

    companion object {
        // ✅ OPTIMASI 7: Improved DiffUtil untuk performa yang lebih baik
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NewsEntity>() {
            override fun areItemsTheSame(oldItem: NewsEntity, newItem: NewsEntity): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: NewsEntity, newItem: NewsEntity): Boolean {
                return oldItem.title == newItem.title &&
                        oldItem.urlToImage == newItem.urlToImage &&
                        oldItem.publishedAt == newItem.publishedAt &&
                        oldItem.sourceName == newItem.sourceName
            }
        }
    }
}
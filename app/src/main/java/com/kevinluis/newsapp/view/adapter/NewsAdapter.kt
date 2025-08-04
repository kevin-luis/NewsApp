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
import com.kevinluis.newsapp.databinding.ItemNewsBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class NewsAdapter(
    private val onItemClick: ((ArticlesItem) -> Unit)? = null
) : ListAdapter<ArticlesItem, NewsAdapter.NewsViewHolder>(DIFF_CALLBACK) {

    // ✅ OPTIMASI 1: Pre-configured Glide options
    private val glideOptions = RequestOptions()
        .override(400, 250)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.image_placeholder)
        .error(R.drawable.broken_image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding, onItemClick, glideOptions)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = getItem(position)
        if (news != null) {
            holder.bind(news)
        }
    }

    // ✅ OPTIMASI 2: ViewHolder dengan optimasi
    class NewsViewHolder(
        private val binding: ItemNewsBinding,
        private val onItemClick: ((ArticlesItem) -> Unit)?,
        private val glideOptions: RequestOptions
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: ArticlesItem) {
            // ✅ OPTIMASI 3: Set click listener hanya sekali
            binding.root.setOnClickListener {
                onItemClick?.invoke(news)
            }

            // ✅ OPTIMASI 4: Optimized image loading
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .apply(glideOptions)
                .into(binding.ivNews)

            // ✅ OPTIMASI 5: Efficient text binding
            binding.tvTitle.text = news.title?.takeIf { it.isNotBlank() }
                ?: "Judul tidak tersedia"

            binding.tvDate.text = try {
                news.publishedAt.let { publishedAt ->
                    DateConverter.convertToIndonesianDateModern(publishedAt)
                } ?: "Tanggal tidak tersedia"
            } catch (e: Exception) {
                "Tanggal tidak valid"
            }

            binding.tvSource.text = news.source.name.takeIf { it.isNotBlank() }
                ?: "Sumber tidak diketahui"

            binding.tvAuthor.text = news.author.takeIf { it.isNotBlank() }
                ?: "Penulis tidak diketahui"
        }
    }

    companion object {
        // ✅ OPTIMASI 6: Improved DiffUtil
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticlesItem>() {
            override fun areItemsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: ArticlesItem, newItem: ArticlesItem): Boolean {
                // ✅ OPTIMASI 7: Streamlined content comparison
                return oldItem.url == newItem.url &&
                        oldItem.title == newItem.title &&
                        oldItem.urlToImage == newItem.urlToImage &&
                        oldItem.publishedAt == newItem.publishedAt
            }

            // ✅ OPTIMASI 8: Optional - payload for partial updates
            override fun getChangePayload(oldItem: ArticlesItem, newItem: ArticlesItem): Any? {
                // Return payload for partial updates if needed
                return if (oldItem.title != newItem.title) "title" else null
            }
        }
    }
}
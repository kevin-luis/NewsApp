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
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.utils.DateConverter

class HeadlineNewsAdapter : ListAdapter<ArticlesItem, HeadlineNewsAdapter.HeadlineNewsViewHolder>(DIFF_CALLBACK){

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HeadlineNewsAdapter.HeadlineNewsViewHolder {
        val binding = ItemHeadlineNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeadlineNewsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: HeadlineNewsAdapter.HeadlineNewsViewHolder,
        position: Int
    ) {
        val headline_news = getItem(position)
        holder.bind(headline_news)
    }

    class HeadlineNewsViewHolder(val binding: ItemHeadlineNewsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(news: ArticlesItem) {
            Glide.with(itemView.context)
                .load(news.urlToImage)
                .override(300)
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.broken_image)
                .into(binding.imageViewBackground)

            binding.textViewTitle.text = news.title ?: "Judul tidak tersedia"
            binding.textViewSource.text = news.source?.name ?: "Sumber tidak diketahui"
            binding.textViewDate.text = news.publishedAt?.let { DateConverter.convertToIndonesianDateModern(it) } ?: "Tanggal tidak tersedia"
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticlesItem> () {
            override fun areItemsTheSame(
                oldItem: ArticlesItem,
                newItem: ArticlesItem
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ArticlesItem,
                newItem: ArticlesItem
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}
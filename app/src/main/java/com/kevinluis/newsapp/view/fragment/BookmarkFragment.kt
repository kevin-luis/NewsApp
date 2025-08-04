package com.kevinluis.newsapp.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.FragmentBookmarkBinding
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.model.remote.response.Source
import com.kevinluis.newsapp.view.adapter.NewsAdapter
import com.kevinluis.newsapp.viewmodel.NewsViewModel
import com.kevinluis.newsapp.viewmodel.ViewModelFactory

class BookmarkFragment : Fragment() {

    private var _binding: FragmentBookmarkBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by activityViewModels {
        ViewModelFactory.getInstance(requireActivity().application)
    }

    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeBookmarkedNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter { article ->
            val bundle = DetailNewsFragment.newInstanceFromArticle(article)

            findNavController().navigate(
                R.id.action_favorite_to_detail,
                bundle
            )
        }

        binding.rvBookmarkNews.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = newsAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeBookmarkedNews() {
        viewModel.getBookmarkedNews().observe(viewLifecycleOwner) { bookmarkedNews ->
            if (bookmarkedNews.isNullOrEmpty()) {
                binding.rvBookmarkNews.visibility = View.GONE
                binding.tvNoBookmarks.visibility = View.VISIBLE
            } else {
                binding.rvBookmarkNews.visibility = View.VISIBLE
                binding.tvNoBookmarks.visibility = View.GONE

                val articles = convertToArticlesItem(bookmarkedNews)
                newsAdapter.submitList(articles)
            }
        }
    }

    private fun convertToArticlesItem(newsEntities: List<NewsEntity>): List<ArticlesItem> {
        return newsEntities.map { newsEntity ->
            ArticlesItem(
                title = newsEntity.title,
                publishedAt = newsEntity.publishedAt ?: "",
                urlToImage = newsEntity.urlToImage ?: "",
                url = newsEntity.url ?: "",
                source = Source(id = "", name = newsEntity.sourceName ?: ""),
                author = newsEntity.author ?: "",
                description = newsEntity.description ?: "",
                content = newsEntity.content ?: ""
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
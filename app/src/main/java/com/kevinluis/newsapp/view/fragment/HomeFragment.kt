package com.kevinluis.newsapp.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevinluis.newsapp.databinding.FragmentHomeBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.model.remote.response.NewsResponse
import com.kevinluis.newsapp.model.remote.retrofit.ApiConfig
import com.kevinluis.newsapp.model.remote.retrofit.NewsAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLayoutManager()
        getNews()
    }


    private fun getNews() {
        val client = ApiConfig.getApiService().getTopHeadlines(COUNTRY)
        client.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        // Filter artikel yang tidak null
                        val articles = responseBody.articles
                        setNewsData(articles)
                    }
                } else {
                    Log.e(TAG, "onFailure: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }
        })
    }


    private fun setupLayoutManager() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvNews.layoutManager = layoutManager

    }

    private fun setNewsData(news: List<ArticlesItem>) {
        val adapter = NewsAdapter()
        adapter.submitList(news)
        binding.rvNews.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val COUNTRY = "us"
        private const val TAG = "HomeFragment"
        private const val QUERY = "Indonesia"
        private const val LANGUAGE = "id"

        private const val EXCLUDE_DOMAIN = "Katalogpromosi.com"
        private const val SORTED_BY = "publishedAt"
    }
}
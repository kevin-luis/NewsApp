package com.kevinluis.newsapp.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.FragmentHomeBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.model.remote.response.NewsResponse
import com.kevinluis.newsapp.model.remote.retrofit.ApiConfig
import com.kevinluis.newsapp.view.adapter.HeadlineNewsAdapter
import com.kevinluis.newsapp.view.adapter.NewsAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var autoScrollRunnable: Runnable
    private var isAutoScrolling = true
    private var currentPage = 0
    private var totalPages = 0
    private var resumeJob: Job? = null

    // Untuk infinite scroll
    private val INFINITE_SCROLL_SIZE = 10000
    private lateinit var headlineNewsAdapter: HeadlineNewsAdapter

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
        setupViewPagerTouchHandler()
        getAllNews()
        getHeadlineNews()
    }

    private fun getAllNews() {
        val client = ApiConfig.getApiService().getEverything(QUERY, LANGUAGE, EXCLUDE_DOMAIN, SORTED_BY)
        client.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
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

    private fun getHeadlineNews() {
        val client = ApiConfig.getApiService().getTopHeadlines(COUNTRY)
        client.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(
                call: Call<NewsResponse>,
                response: Response<NewsResponse>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val articles = responseBody.articles
                        setHeadlineNewsData(articles)
                    }
                } else {
                    Log.e(TAG, "onFailure: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse?>, t: Throwable) {
                Log.e(TAG, "onFailure: ${t.message}")
            }
        })
    }

    private fun setupLayoutManager() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvNews.layoutManager = layoutManager
    }

    private fun setNewsData(news: List<ArticlesItem>) {
        val newsAdapter = NewsAdapter()
        newsAdapter.submitList(news)
        binding.rvNews.adapter = newsAdapter
    }

    private fun setHeadlineNewsData(news: List<ArticlesItem>) {
        if (news.isEmpty()) return

        totalPages = news.size
        headlineNewsAdapter = HeadlineNewsAdapter()

        // Untuk infinite scroll, buat adapter dengan data yang diperbanyak
        val infiniteList = mutableListOf<ArticlesItem>()
        repeat(INFINITE_SCROLL_SIZE / news.size + 1) {
            infiniteList.addAll(news)
        }

        headlineNewsAdapter.submitList(infiniteList)
        binding.vpHeadlineNews.adapter = headlineNewsAdapter

        // Set posisi awal ke tengah untuk infinite scroll
        val startPosition = INFINITE_SCROLL_SIZE / 2
        binding.vpHeadlineNews.setCurrentItem(startPosition, false)
        currentPage = startPosition

        setupViewPagerCallback()
        setupTabLayoutIndicator(news.size)
        startAutoScroll()
    }

    private fun setupViewPagerCallback() {
        binding.vpHeadlineNews.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                // Update indikator berdasarkan posisi real (modulo)
                val realPosition = position % totalPages
                updateIndicator(realPosition)
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        // User mulai drag, pause auto scroll
                        pauseAutoScroll()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        // User selesai drag, resume auto scroll setelah delay
                        resumeAutoScrollWithDelay()
                    }
                }
            }
        })
    }

    private fun setupTabLayoutIndicator(size: Int) {
        // Jika Anda menggunakan TabLayout, gunakan TabLayoutMediator
        // TabLayoutMediator(binding.tabLayout, binding.vpHeadlineNews) { tab, position ->
        //     // Tab akan otomatis sync dengan ViewPager2
        // }.attach()

        // Atau tetap gunakan custom dots seperti sekarang
        setupCustomDots(size)
    }

    private fun setupCustomDots(size: Int) {
        binding.listCircle.removeAllViews()

        repeat(size) { index ->
            val dot = View(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(20, 20).apply {
                    marginStart = 8
                    marginEnd = 8
                }
                background = if (index == 0) {
                    resources.getDrawable(R.drawable.dot_active, null)
                } else {
                    resources.getDrawable(R.drawable.dot_inactive, null)
                }
            }
            binding.listCircle.addView(dot)
        }
    }

    private fun updateIndicator(position: Int) {
        for (i in 0 until binding.listCircle.childCount) {
            val dot = binding.listCircle.getChildAt(i)
            dot.background = if (i == position) {
                resources.getDrawable(R.drawable.dot_active, null)
            } else {
                resources.getDrawable(R.drawable.dot_inactive, null)
            }
        }
    }

    private fun setupViewPagerTouchHandler() {
        // Disable user input saat auto scroll (opsional)
        // binding.vpHeadlineNews.isUserInputEnabled = true
    }

    private fun startAutoScroll() {
        // Stop any existing auto scroll first
        stopAutoScroll()

        if (!::handler.isInitialized) {
            handler = Handler(Looper.getMainLooper())
        }

        isAutoScrolling = true
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isAutoScrolling && totalPages > 1 && isAdded && _binding != null) {
                    currentPage++
                    binding.vpHeadlineNews.setCurrentItem(currentPage, true)
                    handler.postDelayed(this, AUTO_SCROLL_DELAY)
                }
            }
        }
        handler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
    }

    private fun stopAutoScroll() {
        isAutoScrolling = false
        resumeJob?.cancel()
        if (::handler.isInitialized && ::autoScrollRunnable.isInitialized) {
            handler.removeCallbacks(autoScrollRunnable)
        }
    }

    private fun pauseAutoScroll() {
        stopAutoScroll()
    }

    private fun resumeAutoScrollWithDelay() {
        // Cancel any existing resume task
        resumeJob?.cancel()
        resumeJob = lifecycleScope.launch {
            delay(RESUME_DELAY)
            if (isAdded && _binding != null && totalPages > 1) {
                startAutoScroll()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (totalPages > 1) {
            startAutoScroll()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        _binding = null
    }

    companion object {
        private const val COUNTRY = "us"
        private const val TAG = "HomeFragment"
        private const val QUERY = "Indonesia"
        private const val LANGUAGE = "id"
        private const val EXCLUDE_DOMAIN = "Katalogpromosi.com"
        private const val SORTED_BY = "publishedAt"

        // Auto scroll settings
        private const val AUTO_SCROLL_DELAY = 4000L // 4 detik (lebih lambat)
        private const val RESUME_DELAY = 3000L // 3 detik setelah user stop interaksi
    }
}
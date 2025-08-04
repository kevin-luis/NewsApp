package com.kevinluis.newsapp.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.FragmentHomeBinding
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.view.adapter.HeadlineNewsAdapter
import com.kevinluis.newsapp.view.adapter.NewsAdapter
import com.kevinluis.newsapp.viewmodel.NewsViewModel
import com.kevinluis.newsapp.viewmodel.ViewModelFactory
import com.kevinluis.newsapp.model.Result
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var autoScrollRunnable: Runnable
    private var isAutoScrolling = false
    private var currentPage = 0
    private var totalPages = 0
    private var resumeJob: Job? = null
    private lateinit var headlineNewsAdapter: HeadlineNewsAdapter
    private lateinit var newsAdapter: NewsAdapter
    private var isDataLoaded = false

    private val newsViewModel: NewsViewModel by viewModels {
        ViewModelFactory.getInstance(requireActivity())
    }

    // Flag untuk tracking observer registration
    private var observersRegistered = false
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupUI()

        // ✅ PERBAIKAN 3: Hanya register observer sekali
        if (!observersRegistered) {
            setupObservers()
            observersRegistered = true
        }

        loadDataWithCache()
    }

    private fun setupUI() {
        setupLayoutManager()
        setupViewPagerTouchHandler()
        setupAdapters()
    }

    private fun setupAdapters() {
        // Setup NewsAdapter dengan click listener
        newsAdapter = NewsAdapter { article ->
            navigateToDetailNews(article)
        }
        binding.rvNews.adapter = newsAdapter

        // Setup HeadlineNewsAdapter dengan click listener
        headlineNewsAdapter = HeadlineNewsAdapter { newsEntity ->
            navigateToDetailNews(newsEntity)
        }
        binding.vpHeadlineNews.adapter = headlineNewsAdapter
    }

    private fun navigateToDetailNews(article: ArticlesItem) {
        try {
            Log.d(TAG, "Navigating to detail news: ${article.title}")

            val bundle = DetailNewsFragment.newInstanceFromArticle(article)

            // Cek apakah navigation controller ada
            if (findNavController().currentDestination?.id == R.id.navigation_home) {
                findNavController().navigate(
                    R.id.action_home_to_detail,
                    bundle
                )
            } else {
                Log.w(TAG, "Not in home fragment, cannot navigate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to detail", e)
            showErrorMessage("Tidak dapat membuka detail berita")
        }
    }

    private fun navigateToDetailNews(newsEntity: NewsEntity) {
        try {
            Log.d(TAG, "Navigating to detail news: ${newsEntity.title}")

            val bundle = DetailNewsFragment.newInstanceFromEntity(newsEntity)

            // Cek apakah navigation controller ada
            if (findNavController().currentDestination?.id == R.id.navigation_home) {
                findNavController().navigate(
                    R.id.action_home_to_detail,
                    bundle
                )
            } else {
                Log.w(TAG, "Not in home fragment, cannot navigate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to detail", e)
            showErrorMessage("Tidak dapat membuka detail berita")
        }
    }

    // Pisahkan setup observer dari loading data
    private fun setupObservers() {
        // Observer untuk headline news
        newsViewModel.getHeadlineNews().observe(viewLifecycleOwner) { result ->
            handleHeadlineNewsResult(result)
        }

        // Observer untuk all news
        newsViewModel.getAllNewsAlways().observe(viewLifecycleOwner) { result ->
            handleAllNewsResult(result)
        }
    }

    private fun handleHeadlineNewsResult(result: Result<List<NewsEntity>>?) {
        Log.d(TAG, "handleHeadlineNewsResult: ${result?.javaClass?.simpleName}")

        when(result) {
            is Result.Loading -> {
                Log.d(TAG, "Loading headline news...")
                // ✅ OPTIMASI: Hanya show loading pada first load
                if (isFirstLoad) {
                    showHeadlineLoading(true)
                }
            }
            is Result.Success -> {
                Log.d(TAG, "Success loading headline news: ${result.data.size} items")
                showHeadlineLoading(false)
                setHeadlineNewsData(result.data)
                isDataLoaded = true
                isFirstLoad = false
            }
            is Result.Error -> {
                Log.e(TAG, "Error loading headline news: ${result.error}")
                showHeadlineLoading(false)

                // Hanya show error jika first load
                if (isFirstLoad) {
                    showErrorMessage("Terjadi kesalahan: ${result.error}")
                }
                isDataLoaded = false
            }
            null -> {
                Log.d(TAG, "Headline news result is null")
            }
        }
    }

    private fun handleAllNewsResult(result: Result<List<ArticlesItem>>) {
        Log.d(TAG, "handleAllNewsResult: ${result.javaClass.simpleName}")

        when (result) {
            is Result.Loading -> {
                Log.d(TAG, "Loading all news...")
                if (isFirstLoad) {
                    showAllNewsLoading(true)
                }
            }
            is Result.Success -> {
                Log.d(TAG, "Success loading all news: ${result.data.size} items")
                showAllNewsLoading(false)
                setNewsData(result.data)
            }
            is Result.Error -> {
                Log.e(TAG, "Error loading all news: ${result.error}")
                showAllNewsLoading(false)

                if (isFirstLoad) {
                    showErrorMessage("Gagal memuat berita: ${result.error}")
                }
            }
        }
    }

    private fun loadDataWithCache() {
        Log.d(TAG, "Loading data with cache...")

        //Load cached data secara synchronous untuk UI yang responsive
        lifecycleScope.launch {
            // Load cached headline news
            val cachedHeadlineNews = newsViewModel.getCachedHeadlineNews()
            if (cachedHeadlineNews != null && cachedHeadlineNews.isNotEmpty()) {
                Log.d(TAG, "Found cached headline news, displaying immediately")
                setHeadlineNewsData(cachedHeadlineNews)
                isDataLoaded = true
                isFirstLoad = false
            }

            // Load cached all news
            val cachedAllNews = newsViewModel.getCachedAllNews()
            if (cachedAllNews != null && cachedAllNews.isNotEmpty()) {
                Log.d(TAG, "Found cached all news, displaying immediately")
                setNewsData(cachedAllNews)
            }

            // Trigger fresh data load hanya jika cache expired
            if (!newsViewModel.isHeadlineNewsCacheValid() || !newsViewModel.isAllNewsCacheValid()) {
                Log.d(TAG, "Cache expired, triggering fresh data load")
                // Observer akan handle hasil dari fresh data
                newsViewModel.triggerDataLoad()
            }
        }
    }

    private fun showAllNewsLoading(show: Boolean) {
        binding.progressBarAllNews.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvNews.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun showHeadlineLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.vpHeadlineNews.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.listCircle.visibility = if (show) View.INVISIBLE else View.VISIBLE

        if (show) stopAutoScroll()
    }

    private fun showErrorMessage(message: String) {
        // Check jika fragment masih attached
        if (isAdded && _binding != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLayoutManager() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvNews.layoutManager = layoutManager
    }

    private fun setNewsData(news: List<ArticlesItem>) {
        Log.d(TAG, "Setting news data: ${news.size} items")
        if (::newsAdapter.isInitialized) {
            newsAdapter.submitList(news.toList()) // Create new list to force update
        }
    }

    private fun setHeadlineNewsData(news: List<NewsEntity>) {
        if (news.isEmpty()) {
            Log.w(TAG, "Headline news is empty")
            return
        }

        Log.d(TAG, "Setting headline news data: ${news.size} items")
        totalPages = news.size

        if (::headlineNewsAdapter.isInitialized) {
            headlineNewsAdapter.submitList(news.toList()) // Create new list to force update
        }

        currentPage = 0
        binding.vpHeadlineNews.setCurrentItem(0, false) // false untuk smooth navigation

        setupViewPagerCallback()
        setupCustomDots(news.size)

        binding.vpHeadlineNews.visibility = View.VISIBLE
        binding.listCircle.visibility = View.VISIBLE

        // Start auto scroll dengan delay yang lebih pendek
        if (totalPages > 1) {
            lifecycleScope.launch {
                delay(INITIAL_DELAY)
                if (isAdded && _binding != null) {
                    startAutoScroll()
                }
            }
        }
    }

    private fun setupViewPagerCallback() {
        binding.vpHeadlineNews.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateIndicator(position)
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        pauseAutoScroll()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        resumeAutoScrollWithDelay()
                    }
                }
            }
        })
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
        binding.vpHeadlineNews.isUserInputEnabled = true
    }

    private fun startAutoScroll() {
        if (!isDataLoaded) return

        stopAutoScroll()

        if (!::handler.isInitialized) {
            handler = Handler(Looper.getMainLooper())
        }

        isAutoScrolling = true
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isAutoScrolling && totalPages > 1 && isAdded && _binding != null) {
                    currentPage = if (currentPage >= totalPages - 1) 0 else currentPage + 1
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
        resumeJob?.cancel()
        resumeJob = lifecycleScope.launch {
            delay(RESUME_DELAY)
            if (isAdded && _binding != null && totalPages > 1 && isDataLoaded) {
                startAutoScroll()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Fragment resumed")

        // ✅ PERBAIKAN 7: Hanya start auto scroll jika ada data
        if (totalPages > 1 && isDataLoaded) {
            lifecycleScope.launch {
                delay(300) // Delay lebih pendek
                if (isAdded && _binding != null) {
                    startAutoScroll()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Fragment paused")
        stopAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment view destroyed")
        stopAutoScroll()
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        //Reset observer flag ketika fragment destroyed
        observersRegistered = false
        isFirstLoad = true
    }

    companion object {
        private const val TAG = "HomeFragment"
        private const val AUTO_SCROLL_DELAY = 4000L
        private const val RESUME_DELAY = 2000L // Dipercepat
        private const val INITIAL_DELAY = 1000L // Dipercepat
    }
}
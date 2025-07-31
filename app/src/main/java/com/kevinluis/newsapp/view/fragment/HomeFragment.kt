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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private var originalNewsData: List<NewsEntity> = emptyList()

    // ViewModel instance - SHARED across fragment lifecycle
    private val newsViewModel: NewsViewModel by viewModels(
        ownerProducer = { requireActivity() } // Important: Use Activity scope
    ) {
        ViewModelFactory.getInstance(requireActivity())
    }

    // Current data for comparison
    private var currentAllNewsData: List<ArticlesItem>? = null
    private var currentHeadlineData: List<NewsEntity>? = null

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
        loadDataWithCache()
    }

    private fun setupUI() {
        setupLayoutManager()
        setupViewPagerTouchHandler()
        setupSwipeRefresh()
        setupAdapters()
    }

    private fun setupAdapters() {
        // Setup NewsAdapter untuk RecyclerView
        newsAdapter = NewsAdapter { article ->
            // Handle item click - navigate to detail
            Log.d(TAG, "News item clicked: ${article.title}")
        }
        binding.rvNews.adapter = newsAdapter

        // Setup HeadlineNewsAdapter untuk ViewPager
        headlineNewsAdapter = HeadlineNewsAdapter()
        binding.vpHeadlineNews.adapter = headlineNewsAdapter
    }

    private fun setupSwipeRefresh() {
        // Jika menggunakan SwipeRefreshLayout, uncomment dan setup
        /*
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
        */
    }

    private fun loadDataWithCache() {
        Log.d(TAG, "Loading data with cache...")

        // ✅ OPTIMASI: Load cached headline news terlebih dahulu
        val cachedHeadlineNews = newsViewModel.getCachedHeadlineNews()
        if (cachedHeadlineNews != null && cachedHeadlineNews.isNotEmpty()) {
            Log.d(TAG, "Found cached headline news, displaying immediately")
            setHeadlineNewsData(cachedHeadlineNews)
            currentHeadlineData = cachedHeadlineNews
        }

        // ✅ OPTIMASI: Load cached all news terlebih dahulu
        val cachedAllNews = newsViewModel.getCachedAllNews()
        if (cachedAllNews != null && cachedAllNews.isNotEmpty()) {
            Log.d(TAG, "Found cached all news, displaying immediately")
            setNewsData(cachedAllNews)
            currentAllNewsData = cachedAllNews
        }

        // Load fresh data (akan menggunakan cache jika valid)
        getAllNews()
        getHeadlineNews()
    }

    // ✅ OPTIMASI: Update method getHeadlineNews
    private fun getHeadlineNews() {
        newsViewModel.getHeadlineNews().observe(viewLifecycleOwner) { result ->
            Log.d(TAG, "getHeadlineNews result: ${result?.javaClass?.simpleName}")

            when(result) {
                is Result.Loading -> {
                    Log.d(TAG, "Loading headline news...")
                    // ✅ PERBAIKAN: Hanya show loading jika belum ada cached data
                    if (currentHeadlineData == null) {
                        showHeadlineLoading(true)
                    }
                }
                is Result.Success -> {
                    Log.d(TAG, "Success loading headline news: ${result.data.size} items")
                    showHeadlineLoading(false)

                    // ✅ OPTIMASI: Hanya update UI jika data berbeda
                    if (currentHeadlineData != result.data) {
                        setHeadlineNewsData(result.data)
                        currentHeadlineData = result.data
                    }
                    isDataLoaded = true
                }
                is Result.Error -> {
                    Log.e(TAG, "Error loading headline news: ${result.error}")
                    showHeadlineLoading(false)

                    // ✅ PERBAIKAN: Hanya show error jika tidak ada cached data
                    if (currentHeadlineData == null) {
                        showErrorMessage("Terjadi kesalahan: ${result.error}")
                    }
                    isDataLoaded = false
                }
                null -> {
                    Log.d(TAG, "Headline news result is null")
                }
            }
        }
    }

    // ✅ OPTIMASI: Update method getAllNews juga
    private fun getAllNews() {
        newsViewModel.getAllNewsAlways().observe(viewLifecycleOwner) { result ->
            Log.d(TAG, "getAllNews result: ${result.javaClass.simpleName}")

            when (result) {
                is Result.Loading -> {
                    Log.d(TAG, "Loading all news...")
                    // Hanya show loading jika belum ada cached data
                    if (currentAllNewsData == null) {
                        showAllNewsLoading(true)
                    }
                }
                is Result.Success -> {
                    Log.d(TAG, "Success loading all news: ${result.data.size} items")
                    showAllNewsLoading(false)

                    // ✅ OPTIMASI: Hanya update UI jika data berbeda
                    if (currentAllNewsData != result.data) {
                        setNewsData(result.data)
                        currentAllNewsData = result.data
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Error loading all news: ${result.error}")
                    showAllNewsLoading(false)

                    if (currentAllNewsData == null) {
                        showErrorMessage("Gagal memuat berita: ${result.error}")
                    }
                }
            }
        }
    }

    // ✅ TAMBAHKAN: Method untuk refresh semua data
    fun refreshAllData() {
        Log.d(TAG, "Refreshing all data...")

        val (allNewsLiveData, headlineNewsLiveData) = newsViewModel.refreshAllData()

        // Observe refresh all news
        allNewsLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showAllNewsLoading(true)
                is Result.Success -> {
                    showAllNewsLoading(false)
                    setNewsData(result.data)
                    currentAllNewsData = result.data
                }
                is Result.Error -> {
                    showAllNewsLoading(false)
                    showErrorMessage("Gagal memperbarui semua berita")
                }
            }
        }

        // Observe refresh headline news
        headlineNewsLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showHeadlineLoading(true)
                is Result.Success -> {
                    showHeadlineLoading(false)
                    setHeadlineNewsData(result.data)
                    currentHeadlineData = result.data
                    showErrorMessage("Data berhasil diperbarui")
                }
                is Result.Error -> {
                    showHeadlineLoading(false)
                    showErrorMessage("Gagal memperbarui headline")
                }
            }
        }
    }

    private fun showAllNewsLoading(show: Boolean) {
        binding.rvNews.visibility = if (show) View.GONE else View.VISIBLE
        // binding.swipeRefresh.isRefreshing = show
    }

    private fun showHeadlineLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.vpHeadlineNews.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.listCircle.visibility = if (show) View.INVISIBLE else View.VISIBLE

        if (show) stopAutoScroll()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupLayoutManager() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvNews.layoutManager = layoutManager
    }

    private fun setNewsData(news: List<ArticlesItem>) {
        Log.d(TAG, "Setting news data: ${news.size} items")
        newsAdapter.submitList(news.toList()) // Create new list to force update
    }

    private fun setHeadlineNewsData(news: List<NewsEntity>) {
        if (news.isEmpty()) {
            Log.w(TAG, "Headline news is empty")
            return
        }

        Log.d(TAG, "Setting headline news data: ${news.size} items")
        originalNewsData = news
        totalPages = news.size

        headlineNewsAdapter.submitList(news.toList()) // Create new list to force update

        currentPage = 0
        binding.vpHeadlineNews.setCurrentItem(0, false)

        setupViewPagerCallback()
        setupCustomDots(news.size)

        binding.vpHeadlineNews.visibility = View.VISIBLE
        binding.listCircle.visibility = View.VISIBLE

        // Start auto scroll dengan delay
        lifecycleScope.launch {
            delay(INITIAL_DELAY)
            if (isAdded && _binding != null && totalPages > 1) {
                startAutoScroll()
            }
        }
    }

    // Method untuk manual refresh
    fun refreshData() {
        Log.d(TAG, "Manual refresh triggered")

        newsViewModel.refreshAllNews().observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showAllNewsLoading(true)
                is Result.Success -> {
                    showAllNewsLoading(false)
                    setNewsData(result.data)
                    currentAllNewsData = result.data
                    showErrorMessage("Data berhasil diperbarui")
                }
                is Result.Error -> {
                    showAllNewsLoading(false)
                    showErrorMessage("Gagal memperbarui data")
                }
            }
        }

        // Refresh headline news juga
        getHeadlineNews()
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
        if (totalPages > 1 && isDataLoaded) {
            lifecycleScope.launch {
                delay(500)
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

    companion object {
        private const val TAG = "HomeFragment"
        private const val AUTO_SCROLL_DELAY = 4000L
        private const val RESUME_DELAY = 3000L
        private const val INITIAL_DELAY = 1500L
    }
}
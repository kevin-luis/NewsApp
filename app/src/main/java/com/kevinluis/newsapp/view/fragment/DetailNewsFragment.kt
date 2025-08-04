package com.kevinluis.newsapp.view.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kevinluis.newsapp.R
import com.kevinluis.newsapp.databinding.FragmentDetailNewsBinding
import com.kevinluis.newsapp.model.local.entity.NewsEntity
import com.kevinluis.newsapp.model.remote.response.ArticlesItem
import com.kevinluis.newsapp.viewmodel.NewsViewModel
import com.kevinluis.newsapp.viewmodel.ViewModelFactory
import com.kevinluis.newsapp.viewmodel.utils.DateConverter
import androidx.core.net.toUri

class DetailNewsFragment : Fragment() {

    private var _binding: FragmentDetailNewsBinding? = null
    private val binding get() = _binding!!

    private val newsViewModel: NewsViewModel by viewModels {
        ViewModelFactory.getInstance(requireActivity())
    }

    private val glideOptions = RequestOptions()
        .override(800, 400)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.image_placeholder)
        .error(R.drawable.broken_image)

    private var currentNewsEntity: NewsEntity? = null
    private var newsUrl: String? = null

    private var isFromHeadlineNews: Boolean = false

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "Back button pressed, navigating back")
            try {
                if (!findNavController().navigateUp()) {
                    if (!findNavController().popBackStack()) {
                        requireActivity().finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating back", e)
                requireActivity().finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupUI()
        loadDataFromArguments()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            Log.d(TAG, "Toolbar back button clicked")
            handleBackNavigation()
        }
        showLoading(false)
    }

    private fun handleBackNavigation() {
        try {
            val navController = findNavController()
            if (navController.previousBackStackEntry != null) {
                navController.navigateUp()
            } else {
                if (!navController.popBackStack()) {
                    Log.w(TAG, "Cannot navigate back, finishing activity")
                    requireActivity().finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in back navigation", e)
            requireActivity().finish()
        }
    }

    private fun loadDataFromArguments() {
        arguments?.let { args ->
            val title = args.getString(ARG_NEWS_TITLE)
            val url = args.getString(ARG_NEWS_URL)
            val imageUrl = args.getString(ARG_NEWS_IMAGE_URL)
            val source = args.getString(ARG_NEWS_SOURCE)
            val author = args.getString(ARG_NEWS_AUTHOR)
            val publishedAt = args.getString(ARG_NEWS_PUBLISHED_AT)
            val description = args.getString(ARG_NEWS_DESCRIPTION)
            val content = args.getString(ARG_NEWS_CONTENT)

            // Ambil flag sumber data
            isFromHeadlineNews = args.getBoolean(ARG_IS_FROM_HEADLINE, false)

            newsUrl = url

            populateNewsDetail(
                title = title,
                imageUrl = imageUrl,
                source = source,
                author = author,
                publishedAt = publishedAt,
                description = description,
                content = content,
                url = url
            )

            title?.let { checkBookmarkStatus(it) }

        } ?: run {
            Log.e(TAG, "No arguments provided")
            showErrorMessage("Data berita tidak ditemukan")
            handleBackNavigation()
        }
    }

    private fun populateNewsDetail(
        title: String?,
        imageUrl: String?,
        source: String?,
        author: String?,
        publishedAt: String?,
        description: String?,
        content: String?,
        url: String?
    ) {
        binding.apply {
            tvTitle.text = title?.takeIf { it.isNotBlank() }
                ?: "Judul tidak tersedia"

            if (!imageUrl.isNullOrBlank()) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .apply(glideOptions)
                    .into(ivNewsImage)
                ivNewsImage.visibility = View.VISIBLE
            } else {
                ivNewsImage.visibility = View.VISIBLE
                ivNewsImage.setImageResource(R.drawable.image_placeholder)
            }

            val sourceText = source?.takeIf { it.isNotBlank() } ?: "Sumber tidak diketahui"
            val authorText = author?.takeIf { it.isNotBlank() }

            tvSource.text = if (authorText != null) {
                "$sourceText • $authorText"
            } else {
                sourceText
            }

            tvDate.text = try {
                publishedAt?.let { DateConverter.convertToIndonesianDateModern(it) }
                    ?: "Tanggal tidak tersedia"
            } catch (e: Exception) {
                Log.e(TAG, "Error converting date: $publishedAt", e)
                "Tanggal tidak valid"
            }

            if (!description.isNullOrBlank()) {
                tvDescription.text = description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            if (!content.isNullOrBlank()) {
                val cleanContent = content.replace(Regex("\\[\\+\\d+\\s+chars\\]"), "")
                tvContent.text = cleanContent
                tvContent.visibility = View.VISIBLE

                if (content.contains("[+") && !url.isNullOrBlank()) {
                    btnReadMore.visibility = View.VISIBLE
                }
            } else {
                tvContent.visibility = View.GONE
                if (!url.isNullOrBlank()) {
                    btnReadMore.visibility = View.VISIBLE
                    btnReadMore.text = "Baca artikel lengkap"
                }
            }

            if (!url.isNullOrBlank()) {
                tvUrl.text = url.toUri().host ?: url
                tvUrl.visibility = View.VISIBLE
            } else {
                tvUrl.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            fabBookmark.setOnClickListener {
                toggleBookmark()
            }

            fabShare.setOnClickListener {
                shareNews()
            }

            btnReadMore.setOnClickListener {
                openInBrowser()
            }

            tvUrl.setOnClickListener {
                openInBrowser()
            }
        }
    }

    private fun checkBookmarkStatus(title: String) {
        Log.d(TAG, "Checking bookmark status for: $title, isFromHeadline: $isFromHeadlineNews")

        if (isFromHeadlineNews) {
            // Untuk headline news, cek dari cached headline news
            val cachedHeadlineNews = newsViewModel.getCachedHeadlineNews()
            val bookmarkedNews = cachedHeadlineNews?.find { it.title == title }

            if (bookmarkedNews != null) {
                currentNewsEntity = bookmarkedNews
                updateBookmarkIcon(bookmarkedNews.isBookmarked)
                Log.d(TAG, "Found in headline cache, bookmark status: ${bookmarkedNews.isBookmarked}")
                return
            }
        }

        // Untuk semua kasus (headline maupun all news),
        // cek database bookmark dan siapkan NewsEntity
        newsViewModel.getBookmarkedNews().observe(viewLifecycleOwner) { bookmarkedList ->
            val existingBookmark = bookmarkedList.find { it.title == title }

            if (existingBookmark != null) {
                // Jika sudah ada di bookmark, gunakan yang dari database
                currentNewsEntity = existingBookmark
                updateBookmarkIcon(true)
                Log.d(TAG, "Found in bookmarks database")
            } else {
                // Jika belum ada di bookmark, buat NewsEntity baru
                currentNewsEntity = createNewsEntityFromArguments()
                currentNewsEntity?.isBookmarked = false
                updateBookmarkIcon(false)
                Log.d(TAG, "Not bookmarked, created new NewsEntity")
            }
        }
    }

    // Helper method untuk membuat NewsEntity dari arguments
    private fun createNewsEntityFromArguments(): NewsEntity? {
        return arguments?.let { args ->
            NewsEntity(
                title = args.getString(ARG_NEWS_TITLE) ?: return null,
                publishedAt = args.getString(ARG_NEWS_PUBLISHED_AT) ?: "",
                urlToImage = args.getString(ARG_NEWS_IMAGE_URL),
                url = args.getString(ARG_NEWS_URL),
                sourceName = args.getString(ARG_NEWS_SOURCE),
                isBookmarked = false,
                description = args.getString(ARG_NEWS_DESCRIPTION),
                author = args.getString(ARG_NEWS_AUTHOR),
                content = args.getString(ARG_NEWS_CONTENT)
            )
        }
    }

    // Simplify toggleBookmark method
    private fun toggleBookmark() {
        val newsEntity = currentNewsEntity

        if (newsEntity == null) {
            Log.e(TAG, "Cannot toggle bookmark: currentNewsEntity is null")
            showErrorMessage("Tidak dapat menyimpan bookmark")
            return
        }

        val newBookmarkState = !newsEntity.isBookmarked
        Log.d(TAG, "Toggling bookmark for: ${newsEntity.title}, new state: $newBookmarkState")

        // Update database
        newsViewModel.setBookmarkedNews(newsEntity, newBookmarkState)

        // Update local state
        newsEntity.isBookmarked = newBookmarkState
        updateBookmarkIcon(newBookmarkState)

        val message = if (newBookmarkState) {
            "Berita ditambahkan ke Bookmarked"
        } else {
            "Berita dihapus dari Bookmarked"
        }
        showSuccessMessage(message)
    }

    private fun updateBookmarkIcon(isBookmarked: Boolean) {
        binding.fabBookmark.setImageResource(
            if (isBookmarked) {
                R.drawable.ic_bookmark_filled
            } else {
                R.drawable.ic_bookmark_border
            }
        )
        Log.d(TAG, "Updated bookmark icon, isBookmarked: $isBookmarked")
    }

    private fun shareNews() {
        val title = arguments?.getString(ARG_NEWS_TITLE)
        val url = arguments?.getString(ARG_NEWS_URL)

        if (title.isNullOrBlank()) {
            showErrorMessage("Tidak dapat membagikan berita")
            return
        }

        val shareText = if (!url.isNullOrBlank()) {
            "$title\n\nBaca selengkapnya: $url"
        } else {
            title
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Berita Menarik")
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Bagikan berita"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing news", e)
            showErrorMessage("Tidak dapat membagikan berita")
        }
    }

    private fun openInBrowser() {
        val url = arguments?.getString(ARG_NEWS_URL)

        if (url.isNullOrBlank()) {
            showErrorMessage("URL tidak tersedia")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: $url", e)
            showErrorMessage("Tidak dapat membuka tautan")
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        if (isAdded && _binding != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessMessage(message: String) {
        if (isAdded && _binding != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        backPressedCallback.isEnabled = false
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.remove()
    }

    companion object {
        private const val TAG = "DetailNewsFragment"
        const val ARG_NEWS_TITLE = "news_title"
        const val ARG_NEWS_URL = "news_url"
        const val ARG_NEWS_IMAGE_URL = "news_image_url"
        const val ARG_NEWS_SOURCE = "news_source"
        const val ARG_NEWS_AUTHOR = "news_author"
        const val ARG_NEWS_PUBLISHED_AT = "news_published_at"
        const val ARG_NEWS_DESCRIPTION = "news_description"
        const val ARG_NEWS_CONTENT = "news_content"
        const val ARG_IS_FROM_HEADLINE = "is_from_headline"

        fun newInstanceFromEntity(newsEntity: NewsEntity): Bundle {
            return Bundle().apply {
                putString(ARG_NEWS_TITLE, newsEntity.title)
                putString(ARG_NEWS_URL, newsEntity.url)
                putString(ARG_NEWS_IMAGE_URL, newsEntity.urlToImage)
                putString(ARG_NEWS_SOURCE, newsEntity.sourceName)
                putString(ARG_NEWS_PUBLISHED_AT, newsEntity.publishedAt)

                putString(ARG_NEWS_AUTHOR, newsEntity.author)
                putString(ARG_NEWS_DESCRIPTION, newsEntity.description)
                putString(ARG_NEWS_CONTENT, newsEntity.content)

                putBoolean(ARG_IS_FROM_HEADLINE, true)
            }
        }

        fun newInstanceFromArticle(articlesItem: ArticlesItem): Bundle {
            return Bundle().apply {
                putString(ARG_NEWS_TITLE, articlesItem.title)
                putString(ARG_NEWS_URL, articlesItem.url)
                putString(ARG_NEWS_IMAGE_URL, articlesItem.urlToImage)
                putString(ARG_NEWS_SOURCE, articlesItem.source.name)
                putString(ARG_NEWS_AUTHOR, articlesItem.author)
                putString(ARG_NEWS_PUBLISHED_AT, articlesItem.publishedAt)
                putString(ARG_NEWS_DESCRIPTION, articlesItem.description)
                putString(ARG_NEWS_CONTENT, articlesItem.content)
                putBoolean(ARG_IS_FROM_HEADLINE, false)
            }
        }
    }
}
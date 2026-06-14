package com.kraptor

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment(
    private val plugin: SimpCityPlugin,
    private val threadTitle: String,
    private val images: List<String>
) : com.google.android.material.bottomsheet.BottomSheetDialogFragment() {

    private var topBar: View? = null
    private var bottomBar: View? = null
    private var pageCounter: TextView? = null
    private var btnBack: ImageView? = null
    private var btnGrid: ImageView? = null
    private var btnDownload: LinearLayout? = null
    private var btnDownloadAll: LinearLayout? = null
    private var viewPager: ViewPager2? = null
    private var recyclerView: RecyclerView? = null
    
    private var galleryAdapter: GalleryAdapter? = null
    private var gridAdapter: GalleryGridAdapter? = null

    private val downloader by lazy { ImageDownloader(requireContext()) }
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var isChromeVisible = true
    private var isGridView = false
    private val autoHideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoHideRunnable = Runnable { if (isChromeVisible && !isGridView) toggleChrome() }
    private val AUTO_HIDE_DELAY = 4000L

    @SuppressLint("DiscouragedApi")
    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", plugin.resPackageName)
        require(id != 0) { "View ID '$name' not found in ${plugin.resPackageName}" }
        return findViewById(id)
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources!!.getIdentifier("gallery_main", "layout", plugin.resPackageName)
        if (layoutId == 0) return null
        return inflater.inflate(plugin.resources!!.getLayout(layoutId), container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialogInterface ->
            (dialogInterface as? com.google.android.material.bottomsheet.BottomSheetDialog)?.apply {
                findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet).apply {
                        state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                        peekHeight = resources.displayMetrics.heightPixels
                        isDraggable = false
                    }
                    sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }

        if (images.isEmpty()) {
            dismiss()
            return
        }

        try {
            topBar = view.findView<View>("topBar")
            bottomBar = view.findView<View>("bottomBar")
            pageCounter = view.findView("pageCounter")
            btnBack = view.findView("btnBack")
            btnGrid = view.findView("btnGrid")
            btnDownload = view.findView("btnDownload")
            btnDownloadAll = view.findView("btnDownloadAll")
            viewPager = view.findView("galleryPager")
            recyclerView = view.findView("galleryGrid")

            applyProgrammaticStyles()
            setupViewPager()
            setupGridView()
            setupClickListeners()
            setupGlobalKeyListener()

            updatePageCounter(0)
            scheduleAutoHide()
        } catch (e: Exception) {
            e.printStackTrace()
            dismiss()
        }
    }

    private fun applyProgrammaticStyles() {
        val ctx = context ?: return
        val dp = resources.displayMetrics.density

        topBar?.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xCC000000.toInt(), 0x00000000)
        )

        bottomBar?.background = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(0xCC000000.toInt(), 0x00000000)
        )

        val btnBg = { GradientDrawable().apply { cornerRadius = 8f * dp; setColor(Color.TRANSPARENT) } }
        val focusListener = { view: View ->
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus && !isChromeVisible) toggleChrome()
                (v.background as? GradientDrawable)?.apply {
                    if (hasFocus) {
                        setStroke((3 * dp).toInt(), 0xFFFFD600.toInt())
                        setColor(0x1AFFFFFF)
                    } else {
                        setStroke(0, Color.TRANSPARENT)
                        setColor(Color.TRANSPARENT)
                    }
                }
            }
        }

        btnBack?.apply {
            setImageDrawable(createBackArrow(ctx, (24 * dp).toInt()))
            background = btnBg()
            focusListener(this)
        }

        btnGrid?.apply {
            setImageDrawable(createGridIcon(ctx, (24 * dp).toInt()))
            background = btnBg()
            focusListener(this)
        }

        val downloadIcon = btnDownload?.findViewById<ImageView>(
            plugin.resources!!.getIdentifier("btnDownloadIcon", "id", plugin.resPackageName)
        )
        downloadIcon?.setImageDrawable(createDownloadIcon(ctx, (24 * dp).toInt()))
        btnDownload?.apply { background = btnBg(); focusListener(this) }

        val downloadAllIcon = btnDownloadAll?.findViewById<ImageView>(
            plugin.resources!!.getIdentifier("btnDownloadAllIcon", "id", plugin.resPackageName)
        )
        downloadAllIcon?.setImageDrawable(createDownloadAllIcon(ctx, (24 * dp).toInt()))
        btnDownloadAll?.apply { background = btnBg(); focusListener(this) }

        pageCounter?.background = GradientDrawable().apply {
            cornerRadius = 16f * dp
            setColor(0xB3000000.toInt())
        }
    }

    private fun setupViewPager() {
        galleryAdapter = GalleryAdapter(
            plugin = plugin,
            imageUrls = images,
            context = requireContext(),
            onPageChanged = { position -> 
                updatePageCounter(position)
                if (!isGridView) {
                    if (!isChromeVisible) toggleChrome()
                    else scheduleAutoHide()
                }
            },
            onImageClick = {
                if (!isGridView) toggleChrome()
            }
        )

        viewPager?.apply {
            offscreenPageLimit = 2
            adapter = galleryAdapter
            isFocusable = true
            isFocusableInTouchMode = true
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            (getChildAt(0) as? RecyclerView)?.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    galleryAdapter?.onPageSelected(position)
                }
            })
        }
    }

    private fun setupGridView() {
        gridAdapter = GalleryGridAdapter(plugin, images) { position ->
            toggleGridView()
            viewPager?.setCurrentItem(position, false)
        }
        recyclerView?.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack?.setOnClickListener {
            if (isGridView) toggleGridView()
            else dismiss()
        }
        
        btnGrid?.setOnClickListener {
            toggleGridView()
        }

        btnDownload?.setOnClickListener {
            val position = viewPager?.currentItem ?: return@setOnClickListener
            val url = images.getOrNull(position) ?: return@setOnClickListener
            downloadImage(url)
        }

        btnDownloadAll?.setOnClickListener {
            downloadAllImages()
        }
    }

    private fun toggleGridView() {
        isGridView = !isGridView
        
        if (isGridView) {
            viewPager?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            btnDownload?.visibility = View.GONE
            btnDownloadAll?.visibility = View.VISIBLE
            btnGrid?.setImageDrawable(createPagerIcon(requireContext(), (24 * resources.displayMetrics.density).toInt()))
            
            cancelAutoHide()
            if (!isChromeVisible) {
                isChromeVisible = true
                topBar?.visibility = View.VISIBLE
                bottomBar?.visibility = View.VISIBLE
                topBar?.translationY = 0f
                bottomBar?.translationY = 0f
            }
        } else {
            viewPager?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
            btnDownload?.visibility = View.VISIBLE
            btnDownloadAll?.visibility = View.GONE
            btnGrid?.setImageDrawable(createGridIcon(requireContext(), (24 * resources.displayMetrics.density).toInt()))
            scheduleAutoHide()
        }
    }

    private fun getCurrentViewHolder(): GalleryAdapter.GalleryViewHolder? {
        val recyclerView = viewPager?.getChildAt(0) as? RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(viewPager?.currentItem ?: -1) as? GalleryAdapter.GalleryViewHolder
    }

    private fun setupGlobalKeyListener() {
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            
            val focusedView = view?.findFocus()
            val isFromBar = focusedView != null && (
                focusedView == btnBack || focusedView == btnGrid || 
                focusedView == btnDownload || focusedView == btnDownloadAll
            )

            if (isGridView) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    toggleGridView()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }

            // Handle jumps FROM bars
            if (isFromBar) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (focusedView == btnBack || focusedView == btnGrid) {
                            viewPager?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (focusedView == btnDownload || focusedView == btnDownloadAll) {
                            viewPager?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (isChromeVisible) {
                            toggleChrome()
                            viewPager?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                }
                return@setOnKeyListener false // Let default focus system move between buttons
            }

            // If we are here, focus is on Gallery (middle area)
            val holder = getCurrentViewHolder()
            val handledByZoom = holder?.handleDpadKey(keyCode) == true

            if (handledByZoom) return@setOnKeyListener true

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (!isChromeVisible) toggleChrome()
                    btnBack?.requestFocus()
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!isChromeVisible) toggleChrome()
                    btnDownload?.requestFocus()
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    val current = viewPager?.currentItem ?: 0
                    if (current > 0) {
                        viewPager?.setCurrentItem(current - 1, true)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val current = viewPager?.currentItem ?: 0
                    if (current < images.size - 1) {
                        viewPager?.setCurrentItem(current + 1, true)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    toggleChrome()
                    true
                }
                KeyEvent.KEYCODE_BACK -> {
                    dismiss()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleChrome() {
        isChromeVisible = !isChromeVisible
        val top = topBar ?: return
        val bot = bottomBar ?: return

        cancelAutoHide()

        if (isChromeVisible) {
            top.visibility = View.VISIBLE
            bot.visibility = View.VISIBLE
            top.animate().translationY(0f).setDuration(200).start()
            bot.animate().translationY(0f).setDuration(200).start()
            if (!isGridView) scheduleAutoHide()
        } else {
            top.animate().translationY(-top.height.toFloat()).setDuration(200)
                .withEndAction { top.visibility = View.GONE }.start()
            bot.animate().translationY(bot.height.toFloat()).setDuration(200)
                .withEndAction { bot.visibility = View.GONE }.start()
        }
    }

    private fun scheduleAutoHide() {
        cancelAutoHide()
        if (!isGridView) {
            autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY)
        }
    }

    private fun cancelAutoHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
    }

    private fun updatePageCounter(position: Int) {
        pageCounter?.text = "${position + 1} / ${images.size}"
    }

    private fun downloadImage(url: String) {
        Toast.makeText(context, "İndirme başlıyor...", Toast.LENGTH_SHORT).show()
        uiScope.launch {
            val fullUrl = ImageUrlFilter.upgradeToFullQuality(url)
            val result = downloader.downloadToDownloads(fullUrl, threadTitle)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    Toast.makeText(context, "Kaydedildi: Download/SimpCity/$threadTitle", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "İndirme başarısız", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAllImages() {
        Toast.makeText(context, "Tüm görseller indiriliyor (${images.size})...", Toast.LENGTH_LONG).show()
        uiScope.launch {
            var count = 0
            images.forEach { url ->
                val fullUrl = ImageUrlFilter.upgradeToFullQuality(url)
                val res = downloader.downloadToDownloads(fullUrl, threadTitle)
                if (res != null) count++
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "$count / ${images.size} görsel kaydedildi.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelAutoHide()
        uiScope.cancel()
        galleryAdapter = null
        gridAdapter = null
        viewPager = null
        recyclerView = null
    }

    companion object {
        private fun createBackArrow(context: android.content.Context, sizePx: Int): BitmapDrawable {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = sizePx * 0.1f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
            val p = sizePx * 0.2f
            val midY = sizePx / 2f
            val path = Path().apply { moveTo(sizePx - p, p); lineTo(p, midY); lineTo(sizePx - p, sizePx - p) }
            canvas.drawPath(path, paint)
            return BitmapDrawable(context.resources, bmp)
        }

        private fun createGridIcon(context: android.content.Context, sizePx: Int): BitmapDrawable {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
            val gap = sizePx * 0.1f
            val s = (sizePx - 2 * gap) / 3f
            for (i in 0..2) {
                for (j in 0..2) {
                    canvas.drawRect(i * (s + gap), j * (s + gap), i * (s + gap) + s, j * (s + gap) + s, paint)
                }
            }
            return BitmapDrawable(context.resources, bmp)
        }

        private fun createPagerIcon(context: android.content.Context, sizePx: Int): BitmapDrawable {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = sizePx * 0.08f }
            val p = sizePx * 0.15f
            canvas.drawRect(p, p, sizePx - p, sizePx - p, paint)
            canvas.drawLine(p, sizePx / 2f, sizePx - p, sizePx / 2f, paint)
            return BitmapDrawable(context.resources, bmp)
        }

        private fun createDownloadIcon(context: android.content.Context, sizePx: Int): BitmapDrawable {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = sizePx * 0.1f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
            val p = sizePx * 0.2f
            val midX = sizePx / 2f
            val arrow = Path().apply { moveTo(midX, p); lineTo(midX, sizePx - p * 1.5f); moveTo(p, sizePx - p * 2.5f); lineTo(midX, sizePx - p * 1.5f); lineTo(sizePx - p, sizePx - p * 2.5f) }
            canvas.drawPath(arrow, paint)
            canvas.drawLine(p, sizePx - p, sizePx - p, sizePx - p, paint)
            return BitmapDrawable(context.resources, bmp)
        }

        private fun createDownloadAllIcon(context: android.content.Context, sizePx: Int): BitmapDrawable {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = sizePx * 0.08f; strokeCap = Paint.Cap.ROUND }
            val p = sizePx * 0.2f
            val midX = sizePx / 2f
            val drawArrow = { offset: Float ->
                val path = Path().apply { moveTo(midX, p + offset); lineTo(midX, sizePx - p * 1.5f + offset); moveTo(p, sizePx - p * 2.5f + offset); lineTo(midX, sizePx - p * 1.5f + offset); lineTo(sizePx - p, sizePx - p * 2.5f + offset) }
                canvas.drawPath(path, paint)
            }
            drawArrow(-sizePx * 0.15f)
            drawArrow(sizePx * 0.05f)
            canvas.drawLine(p, sizePx - p * 0.5f, sizePx - p, sizePx - p * 0.5f, paint)
            return BitmapDrawable(context.resources, bmp)
        }
    }
}

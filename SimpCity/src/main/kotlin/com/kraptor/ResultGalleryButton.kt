package com.kraptor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import java.lang.ref.WeakReference

object ResultGalleryButton {
    private const val TAG_ID = "simpcity_gallery_button"
    private const val API_NAME = "SimpCity"

    private val galleryCache = mutableMapOf<String, Pair<String, List<String>>>()
    private val liveButtons = mutableMapOf<String, WeakReference<View>>()

    private var registeredManager: FragmentManager? = null

    fun register(plugin: SimpCityPlugin, activity: AppCompatActivity) {
        val fm = activity.supportFragmentManager
        if (registeredManager === fm) return
        registeredManager = fm

        fm.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
                ) {
                    val args = f.arguments ?: return
                    if (args.getString("apiName") != API_NAME) return
                    val url = args.getString("url") ?: return
                    runCatching { injectButton(plugin, v, url) }
                }
            },
            true
        )
    }

    fun cacheGallery(url: String, title: String, images: List<String>) {
        if (images.isEmpty()) return
        galleryCache[url] = title to images
        Handler(Looper.getMainLooper()).post {
            liveButtons[url]?.get()?.visibility = View.VISIBLE
        }
    }

    private fun injectButton(plugin: SimpCityPlugin, root: View, url: String) {
        val context = root.context
        val pkg = context.packageName
        fun id(name: String) = context.resources.getIdentifier(name, "id", pkg)

        val onClick: (View) -> Unit = {
            galleryCache[url]?.let { (title, images) -> plugin.loadGallery(title, images) }
        }

        val tvParentId = id("result_play_parent")
        val parent = if (tvParentId != 0) root.findViewById<LinearLayout>(tvParentId) else null
        if (parent != null && parent.findViewWithTag<View>(TAG_ID) == null) {
            val anchor = parent.findViewById<View>(id("result_bookmark"))
            val button = buildTvButton(context, pkg, onClick)
            val index = if (anchor != null) parent.indexOfChild(anchor) + 1 else parent.childCount
            parent.addView(button, index)
            button.visibility = if (galleryCache.containsKey(url)) View.VISIBLE else View.GONE
            liveButtons[url] = WeakReference(button)
            return
        }

        val phoneHolderId = id("media_route_button_holder")
        val holder = if (phoneHolderId != 0) root.findViewById<LinearLayout>(phoneHolderId) else null
        if (holder != null && holder.findViewWithTag<View>(TAG_ID) == null) {
            val anchor = holder.findViewById<View>(id("result_search"))
            val button = buildPhoneButton(context, onClick)
            val index = if (anchor != null) holder.indexOfChild(anchor) + 1 else holder.childCount
            holder.addView(button, index)
            button.visibility = if (galleryCache.containsKey(url)) View.VISIBLE else View.GONE
            liveButtons[url] = WeakReference(button)
        }
    }

    private fun buildPhoneButton(context: Context, onClick: (View) -> Unit): View {
        val dp = context.resources.displayMetrics.density
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)

        return ImageView(context).apply {
            tag = TAG_ID
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams((25 * dp).toInt(), (25 * dp).toInt()).apply {
                marginStart = (5 * dp).toInt()
                marginEnd = (5 * dp).toInt()
                gravity = Gravity.CENTER_VERTICAL
            }
            if (outValue.resourceId != 0) setBackgroundResource(outValue.resourceId)
            setImageDrawable(createGalleryIcon(context, (25 * dp).toInt(), Color.WHITE))
            contentDescription = "Galeri"
            isFocusable = true
            isClickable = true
            setOnClickListener(onClick)
        }
    }

    private fun buildTvButton(context: Context, pkg: String, onClick: (View) -> Unit): View {
        val dp = context.resources.displayMetrics.density
        val textStyleId = context.resources.getIdentifier("ResultMarqueeButtonText", "style", pkg)

        val wrapper = LinearLayout(context).apply {
            tag = TAG_ID
            visibility = View.GONE
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (8 * dp).toInt() }
        }

        val button = MaterialButton(context).apply {
            isFocusable = true
            icon = createGalleryIcon(context, (20 * dp).toInt(), Color.WHITE)
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            minWidth = 0
            minimumWidth = 0
            insetTop = 0
            insetBottom = 0
            cornerRadius = (8 * dp).toInt()
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            text = ""
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.argb(140, 40, 40, 40))
            setOnClickListener(onClick)
        }

        val label = (if (textStyleId != 0) {
            TextView(context, null, 0, textStyleId)
        } else {
            TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
            }
        }).apply { text = "Galeri" }

        wrapper.addView(button)
        wrapper.addView(label)
        return wrapper
    }

    private fun createGalleryIcon(context: Context, sizePx: Int, color: Int): BitmapDrawable {
        val size = sizePx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val pad = size * 0.12f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.08f
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawRoundRect(RectF(pad, pad, size - pad, size - pad), size * 0.08f, size * 0.08f, strokePaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
        canvas.drawCircle(size * 0.32f, size * 0.32f, size * 0.07f, fillPaint)

        val mountain = Path().apply {
            moveTo(pad * 1.6f, size - pad * 1.6f)
            lineTo(size * 0.42f, size * 0.5f)
            lineTo(size * 0.6f, size * 0.68f)
            lineTo(size * 0.76f, size * 0.46f)
            lineTo(size - pad * 1.6f, size - pad * 1.6f)
            close()
        }
        canvas.drawPath(mountain, fillPaint)

        return BitmapDrawable(context.resources, bmp)
    }
}

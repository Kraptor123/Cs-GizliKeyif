package com.kraptor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.api.Log

// BuildConfig YOK — plugin.resPackageName kullanılıyor
class GalleryFragment(
    private val plugin: SimpCityPlugin,
    private val images: List<String>
) : BottomSheetDialogFragment() {

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
        Log.d("kraptor_SimpCity", "GalleryFragment.onCreateView pkg=${plugin.resPackageName}")
        val layoutId = plugin.resources!!.getIdentifier("chapter", "layout", plugin.resPackageName)
        if (layoutId == 0) {
            Log.e("kraptor_SimpCity", "Layout 'chapter' NOT FOUND in ${plugin.resPackageName}!")
            return null
        }
        return inflater.inflate(plugin.resources!!.getLayout(layoutId), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("kraptor_SimpCity", "GalleryFragment.onViewCreated images=${images.size}")

        dialog?.setOnShowListener { dialogInterface ->
            (dialogInterface as? BottomSheetDialog)?.apply {
                findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                    BottomSheetBehavior.from(sheet).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        peekHeight = resources.displayMetrics.heightPixels
                        isDraggable = false
                    }
                    sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }

        if (images.isEmpty()) {
            Log.e("kraptor_SimpCity", "GalleryFragment: images empty, dismissing")
            dismiss()
            return
        }

        try {
            val recyclerView = view.findView<RecyclerView>("page_list")
            Log.d("kraptor_SimpCity", "GalleryFragment: RecyclerView found")

            recyclerView.apply {
                setHasFixedSize(true)
                setItemViewCacheSize(10)
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
                    initialPrefetchItemCount = 3
                }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            (recyclerView.adapter as? GalleryAdapter)?.preloadAdjacent(recyclerView)
                        }
                    }
                })
            }

            PagerSnapHelper().attachToRecyclerView(recyclerView)
            recyclerView.adapter = GalleryAdapter(plugin, images, requireContext())
            Log.d("kraptor_SimpCity", "GalleryFragment: adapter set, gallery ready!")
        } catch (e: Exception) {
            Log.e("kraptor_SimpCity", "GalleryFragment.onViewCreated FAILED: ${e.message}")
            e.printStackTrace()
            dismiss()
        }
    }
}

// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import org.json.JSONArray

object SpeedAyarlar {
    private const val PREFS_PREFIX = "Speedporn_"
    const val ALL_CATEGORIES_ORDER_KEY = "${PREFS_PREFIX}ALL_order"

    private const val COLOR_BLACK = "#000000"
    private const val COLOR_WHITE = "#FFFFFF"

    private val defaultCategories = listOf(
        "movies" to "Latest Release",
        "movies/random" to "Random Contents",
        "genre/18-teens" to "18+ Teens",
        "genre/all-girl" to "All Girl",
        "genre/all-sex" to "All Sex",
        "genre/anal" to "Anal",
        "genre/asian" to "Asian",
        "genre/bbc" to "BBC",
        "genre/bbw" to "BBW",
        "genre/big-boobs" to "Big Boobs",
        "genre/big-butt" to "Big Butt",
        "genre/big-cock" to "Big Cock",
        "genre/big-cocks" to "Big Cocks",
        "genre/bdsm" to "BDSM",
        "genre/blondes" to "Blondes",
        "genre/blowjobs" to "Blowjobs",
        "genre/bondage" to "Bondage",
        "genre/cuckolds" to "Cuckolds",
        "genre/cumshots" to "Cumshots",
        "genre/deep-throat" to "Deep Throat",
        "genre/double-anal" to "Double Anal",
        "genre/double-penetration" to "Double Penetration",
        "genre/facials" to "Facials",
        "genre/family-roleplay" to "Family Roleplay",
        "genre/fantasy" to "Fantasy",
        "genre/fetish" to "Fetish",
        "genre/france" to "French",
        "genre/free-use" to "FreeUSE",
        "genre/gangbang" to "Gangbang",
        "genre/germany" to "German",
        "genre/gonzo" to "Gonzo",
        "genre/group-sex" to "Group Sex",
        "genre/interracial" to "Interracial",
        "genre/lesbian" to "Lesbian",
        "genre/lingerie" to "Lingerie",
        "genre/mature" to "Mature",
        "genre/milf" to "MILF",
        "genre/parody" to "Parody",
        "genre/pregnant" to "Pregnant",
        "genre/public-sex" to "Public Sex",
        "genre/redheads" to "Red Heads",
        "genre/russian" to "Russian",
        "genre/small-tits" to "Small Tits",
        "genre/squirting" to "Squirting",
        "genre/stockings" to "Stockings",
        "genre/swallowing" to "Swallowing",
        "genre/swingers" to "Swingers",
        "genre/threesomes" to "Threesomes",
        "genre/wives" to "Wives",
        "xxx/studios/21-sextury-video" to "21 Sextury",
        "xxx/studios/3rd-degree" to "3RD Degree",
        "xxx/studios/adam-eve" to "Adam & Eve",
        "xxx/studios/amk-empire" to "AMK Empire",
        "xxx/studios/bang-bros-productions" to "Bang Bros Productions",
        "xxx/studios/brazzers" to "Brazzers",
        "xxx/studios/bluebird-films" to "Bluebird Films",
        "xxx/studios/cento-x-cento" to "CentoXCento",
        "xxx/studios/combat-zone" to "Combat Zone",
        "xxx/studios/ddf-network" to "DDF Network",
        "xxx/studios/devils-film" to "Devil’s Film",
        "xxx/studios/digital-playground" to "Digital Playground",
        "xxx/studios/digital-sin" to "Digital Sin",
        "xxx/studios/diabolic-video" to "Diabolic Video",
        "xxx/studios/elegant-angel" to "Elegant Angel",
        "xxx/studios/evil-angel" to "Evil Angel",
        "xxx/studios/evasive-angles" to "Evasive Angles",
        "xxx/studios/fun-movies" to "Fun Movies Studio",
        "xxx/studios/ggg" to "GGG",
        "xxx/studios/girlfriends-films" to "Girlfriends Films",
        "xxx/studios/hustler" to "Hustler",
        "xxx/studios/jules-jordan-video" to "Jules Jordan",
        "xxx/studios/lethal-hardcore" to "Lethal Hardcore",
        "xxx/studios/magma-film" to "Magma Film",
        "xxx/studios/marc-dorcel" to "Marc Dorcel",
        "xxx/studios/mmv" to "MMV",
        "xxx/studios/mofos" to "MOFOS",
        "xxx/studios/naughty-america" to "Naughty America",
        "xxx/studios/new-sensations" to "New Sensations",
        "xxx/studios/paradise-film" to "Paradise Film",
        "xxx/studios/penthouse" to "Penthouse",
        "xxx/studios/porn-pros" to "Porn Pros",
        "xxx/studios/private" to "Private",
        "xxx/studios/reality-kings" to "Reality Kings",
        "xxx/studios/team-skeet" to "Team Skeet",
        "xxx/studios/united-house-brands" to "United House Brands",
        "xxx/studios/wicked-pictures" to "Wicked Pictures",
        "xxx/studios/white-ghetto" to "White Ghetto",
        "xxx/studios/zero-tolerance" to "Zero Tolerance",
        "trending/" to "Trending",
        "ratings" to "Rating",
        "xxxmovies/genre/indian" to "Indian",
        "xxxxmovies/studios/asia-bootleg" to "Asia Bootleg",
        "xxxmovies/genre/bathroom" to "Bathroom",
        "xxxmovies/genre/tattoos" to "Tattoos"
    ) + (1980..2025).map { "year/$it" to "$it" }.sortedBy { it.second.lowercase() }

    private val defaultEnabledNames = setOf(
        "Latest Release", "Random Contents", "German", "Russian", "French"
    )

    private fun dpToPx(c: Context, dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), c.resources.displayMetrics).toInt()
    }

    fun getOrderedAndEnabledCategories(): List<Pair<String, String>> {
        val allCats = defaultCategories
        val allCategoryNames = allCats.map { it.second }
        val orderedNames = getOrderedCategories(ALL_CATEGORIES_ORDER_KEY, allCategoryNames)
        return orderedNames
            .filter { name -> isCategoryEnabled(name) }
            .mapNotNull { name -> allCats.find { it.second == name } }
    }

    fun isCategoryEnabled(categoryName: String): Boolean {
        val key = "${PREFS_PREFIX}${categoryName}_enabled"
        return when (getKey<String>(key)) {
            "true"  -> true
            "false" -> false
            else    -> defaultEnabledNames.contains(categoryName)
        }
    }

    fun setCategoryEnabled(categoryName: String, enabled: Boolean) {
        setKey("${PREFS_PREFIX}${categoryName}_enabled", enabled.toString())
    }

    fun getOrderedCategories(key: String, defaultList: List<String>): List<String> {
        val savedOrderJson: String? = getKey(key)
        return if (savedOrderJson != null) {
            try {
                val jsonArray = JSONArray(savedOrderJson)
                val savedList = (0 until jsonArray.length()).map { jsonArray.getString(it) }
                val defaultSet = defaultList.toSet()
                val validSavedList = savedList.filter { it in defaultSet }
                val newItems = defaultList.filter { it !in validSavedList }
                validSavedList + newItems
            } catch (_: Exception) {
                defaultList
            }
        } else {
            defaultList
        }
    }

    fun setOrderedCategories(key: String, list: List<String>) {
        val jsonArray = JSONArray().apply { list.forEach { put(it) } }
        setKey(key, jsonArray.toString())
    }

    fun resetAllSettings() {
        setKey(ALL_CATEGORIES_ORDER_KEY, null)
        defaultCategories.forEach { (_, name) ->
            setKey("${PREFS_PREFIX}${name}_enabled", null)
        }
    }

    fun showSettingsDialog(activity: AppCompatActivity, onSave: () -> Unit) {
        SettingsManager(activity, onSave).show()
    }

    private class SettingsManager(val context: AppCompatActivity, val onSave: () -> Unit) {
        private var dialog: AlertDialog? = null
        private lateinit var adapter: CategoryAdapter

        private val focusTextColorList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(-android.R.attr.state_focused)
            ),
            intArrayOf(
                Color.parseColor(COLOR_BLACK),
                Color.parseColor(COLOR_WHITE)
            )
        )

        fun show() {
            val mainLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor(COLOR_BLACK))
                setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
                isFocusable = false
                isClickable = false
            }

            val title = TextView(context).apply {
                text = "Categories"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(COLOR_WHITE))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(context, 12), 0, dpToPx(context, 12))
                isFocusable = false
            }
            mainLayout.addView(title)

            adapter = CategoryAdapter(context) { name, enabled ->
                setCategoryEnabled(name, enabled)
                refreshList()
            }

            val rv = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter  = this@SettingsManager.adapter
                setPadding(dpToPx(context, 8), 0, dpToPx(context, 8), 0)
                clipToPadding = false
                isFocusable   = false
                isClickable   = false
                layoutParams  = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            mainLayout.addView(rv)

            val footer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(context, 12), 0, dpToPx(context, 8))
                gravity = Gravity.CENTER
                isFocusable = false
                isClickable = false
            }

            val btnReset = createActionButton("Reset") {
                resetAllSettings()
                refreshList()
            }
            val btnSave = createActionButton("Save and Exit") {
                onSave()
                dialog?.dismiss()
            }

            footer.addView(btnReset)
            footer.addView(btnSave)
            mainLayout.addView(footer)

            dialog = AlertDialog.Builder(context)
                .setView(mainLayout)
                .setCancelable(true)
                .setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        onSave()
                        dialog?.dismiss()
                        true
                    } else {
                        false
                    }
                }
                .create()

            dialog?.show()

            val window = dialog?.window
            val displayMetrics = context.resources.displayMetrics
            window?.setLayout((displayMetrics.widthPixels * 0.92).toInt(), (displayMetrics.heightPixels * 0.88).toInt())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(0f)
            window?.setBackgroundDrawable(ColorDrawable(Color.parseColor(COLOR_BLACK)))

            refreshList()
        }

        private fun createActionButton(txt: String, onClick: (View) -> Unit) = Button(context).apply {
            text = txt
            setTextColor(focusTextColorList)
            setTypeface(null, Typeface.BOLD)
            textSize = 14f
            isFocusable = true
            isClickable = true
            background = createBWDrawable(radiusPx = dpToPx(context, 24))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 48), 1f).apply {
                marginStart = dpToPx(context, 8)
                marginEnd   = dpToPx(context, 8)
            }
            setOnClickListener { onClick(it) }
        }

        private fun refreshList() {
            val names        = defaultCategories.map { it.second }
            val ordered      = getOrderedCategories(ALL_CATEGORIES_ORDER_KEY, names)
            val enabledList  = ordered.filter { isCategoryEnabled(it) }
            val disabledList = ordered.filter { !isCategoryEnabled(it) }
            val sortedList   = enabledList + disabledList
            adapter.setList(sortedList)
        }

        private fun createBWDrawable(radiusPx: Int): Drawable {
            val focusedDrawable = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_WHITE))
                cornerRadius = radiusPx.toFloat()
            }
            val normalDrawable = GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_BLACK))
                cornerRadius = radiusPx.toFloat()
            }
            return StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
                addState(intArrayOf(), normalDrawable)
            }
        }

        private inner class CategoryAdapter(val ctx: Context, val onCheckedChange: (String, Boolean) -> Unit) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
            private val items = mutableListOf<String>()

            fun setList(newList: List<String>) {
                items.clear()
                items.addAll(newList)
                notifyDataSetChanged()
            }

            inner class ViewHolder(v: View, val cb: CheckBox, val up: Button, val down: Button) : RecyclerView.ViewHolder(v)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = Gravity.CENTER_VERTICAL
                    val p       = dpToPx(ctx, 4)
                    setPadding(p, p, p, p)
                    val m       = dpToPx(ctx, 4)
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(m, m, m, m)
                    }
                    isFocusable = false
                    isClickable = false
                }

                val cb = CheckBox(ctx).apply {
                    setTextColor(focusTextColorList)
                    buttonTintList = ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            Color.parseColor(COLOR_WHITE),
                            Color.parseColor("#888888")
                        )
                    )
                    textSize    = 16f
                    isFocusable = true
                    isClickable = true
                    val p       = dpToPx(ctx, 8)
                    setPadding(p, p, p, p)
                    background  = createBWDrawable(radiusPx = dpToPx(ctx, 8))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(cb)

                val bSize = dpToPx(ctx, 44)
                val up = Button(ctx).apply {
                    text        = "▲"
                    setTextColor(focusTextColorList)
                    textSize    = 16f
                    setTypeface(null, Typeface.BOLD)
                    isFocusable = true
                    isClickable = true
                    background  = createBWDrawable(radiusPx = dpToPx(ctx, 22))
                    layoutParams = LinearLayout.LayoutParams(bSize, bSize).apply {
                        marginStart = dpToPx(ctx, 6)
                        marginEnd   = dpToPx(ctx, 4)
                    }
                }
                row.addView(up)

                val down = Button(ctx).apply {
                    text        = "▼"
                    setTextColor(focusTextColorList)
                    textSize    = 16f
                    setTypeface(null, Typeface.BOLD)
                    isFocusable = true
                    isClickable = true
                    background  = createBWDrawable(radiusPx = dpToPx(ctx, 22))
                    layoutParams = LinearLayout.LayoutParams(bSize, bSize)
                }
                row.addView(down)

                return ViewHolder(row, cb, up, down)
            }

            override fun getItemCount(): Int = items.size

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val name = items[position]

                holder.cb.setOnCheckedChangeListener(null)
                holder.cb.text      = name
                holder.cb.isChecked = isCategoryEnabled(name)

                holder.cb.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(name, isChecked)
                }

                holder.up.setOnClickListener { move(holder.adapterPosition, holder.adapterPosition - 1) }
                holder.down.setOnClickListener { move(holder.adapterPosition, holder.adapterPosition + 1) }
            }

            private fun move(from: Int, to: Int) {
                if (from < 0 || to !in items.indices) return
                val item = items.removeAt(from)
                items.add(to, item)
                notifyItemMoved(from, to)
                setOrderedCategories(ALL_CATEGORIES_ORDER_KEY, items)
            }
        }
    }
}

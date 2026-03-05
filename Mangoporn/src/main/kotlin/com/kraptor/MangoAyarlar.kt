package com.kraptor

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import org.json.JSONArray

object MangoAyarlar {
    private const val PREFS_PREFIX = "Mangoporn_"
    const val ALL_CATEGORIES_ORDER_KEY = "${PREFS_PREFIX}ALL_order"

    private const val COLOR_BG = "#0A0A0A"
    private const val COLOR_PRIMARY = "#8842f3"
    private const val COLOR_FOCUS = "#00D9FF"
    private const val COLOR_DELETE = "#D32F2F"
    private const val COLOR_SAVE = "#2E7D32"
    private const val COLOR_CARD = "#1A1A1A"

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
        "genre/germany" to "Germany",
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

    fun dpToPx(c: Context, dp: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), c.resources.displayMetrics).toInt()

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
            "true" -> true
            "false" -> false
            else -> defaultEnabledNames.contains(categoryName)
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
            } catch (e: Exception) {
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
        private val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(COLOR_BG))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        private lateinit var adapter: CategoryAdapter
        private var dialog: AlertDialog? = null

        fun show() {
            dialog = AlertDialog.Builder(context)
                .setView(createRootView())
                .setCancelable(false)
                .create()
            dialog?.show()

            val window = dialog?.window
            val displayMetrics = context.resources.displayMetrics
            window?.setLayout((displayMetrics.widthPixels * 0.92).toInt(), (displayMetrics.heightPixels * 0.88).toInt())
            window?.setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_BG))
                cornerRadius = 32f
                setStroke(3, Color.parseColor(COLOR_PRIMARY))
            })
        }

        private fun createRootView(): View {
            val title = TextView(context).apply {
                text = "CATEGORY SETTINGS"
                textSize = 22f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 50, 0, 50)
            }
            mainLayout.addView(title)

            adapter = CategoryAdapter(context) { name, enabled -> setCategoryEnabled(name, enabled) }
            val rv = RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                this.adapter = this@SettingsManager.adapter
                setPadding(20, 0, 20, 0)
                clipToPadding = false
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            mainLayout.addView(rv)

            val footer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(40, 30, 40, 40)
                gravity = Gravity.CENTER
            }

            val btnReset = createActionButton("RESET ALL", COLOR_DELETE) {
                resetAllSettings()
                refreshList()
            }
            val btnSave = createActionButton("SAVE & EXIT", COLOR_SAVE) {
                onSave()
                dialog?.dismiss()
            }

            footer.addView(btnReset)
            footer.addView(btnSave)
            mainLayout.addView(footer)

            refreshList()
            return mainLayout
        }

        private fun createActionButton(txt: String, color: String, onClick: (View) -> Unit) = Button(context).apply {
            text = txt
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            textSize = 14f
            background = createButtonDrawable(Color.parseColor(color))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(context, 50), 1f).apply {
                marginStart = 20
                marginEnd = 20
            }
            setOnClickListener { onClick(it) }
        }

        private fun refreshList() {
            val names = defaultCategories.map { it.second }
            val ordered = getOrderedCategories(ALL_CATEGORIES_ORDER_KEY, names)
            adapter.setList(ordered)
        }

        private fun createButtonDrawable(color: Int) = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
                setColor(Color.parseColor(COLOR_FOCUS))
                cornerRadius = 16f
                setStroke(4, Color.WHITE)
            })
            addState(intArrayOf(), GradientDrawable().apply {
                setColor(color)
                cornerRadius = 16f
            })
        }

        private inner class CategoryAdapter(val ctx: Context, val onCheckedChange: (String, Boolean) -> Unit) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
            private val items = mutableListOf<String>()
            private val CHECKBOX_ID = View.generateViewId()
            private val UP_ID = View.generateViewId()
            private val DOWN_ID = View.generateViewId()

            fun setList(newList: List<String>) {
                items.clear()
                items.addAll(newList)
                notifyDataSetChanged()
            }

            inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
                val cb = v.findViewById<CheckBox>(CHECKBOX_ID)
                val up = v.findViewById<Button>(UP_ID)
                val down = v.findViewById<Button>(DOWN_ID)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(createItemLayout(parent.context))
            override fun getItemCount() = items.size
            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val name = items[position]

                holder.cb.setOnCheckedChangeListener(null)

                holder.cb.text = name
                holder.cb.isChecked = isCategoryEnabled(name)

                holder.cb.setOnCheckedChangeListener { _, isChecked ->
                    setCategoryEnabled(name, isChecked)
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

            private fun createItemLayout(c: Context) = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = dpToPx(c, 12)
                setPadding(p, p, p, p)
                val margin = dpToPx(c, 6)
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
                        setColor(Color.parseColor(COLOR_CARD))
                        cornerRadius = 12f
                        setStroke(2, Color.parseColor(COLOR_FOCUS))
                    })
                    addState(intArrayOf(), GradientDrawable().apply {
                        setColor(Color.parseColor(COLOR_CARD))
                        cornerRadius = 12f
                    })
                }

                addView(CheckBox(c).apply {
                    id = CHECKBOX_ID
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    buttonTintList = ColorStateList.valueOf(Color.parseColor(COLOR_PRIMARY))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    isFocusable = true
                    setOnFocusChangeListener { _, hasFocus ->
                        setTextColor(if (hasFocus) Color.parseColor(COLOR_FOCUS) else Color.WHITE)
                    }
                })

                val bSize = dpToPx(c, 42)
                addView(createNavButton(c, "▲", UP_ID).apply { layoutParams = LinearLayout.LayoutParams(bSize, bSize).apply { marginEnd = 10 } })
                addView(createNavButton(c, "▼", DOWN_ID).apply { layoutParams = LinearLayout.LayoutParams(bSize, bSize) })
            }

            private fun createNavButton(c: Context, symbol: String, btnId: Int) = Button(c).apply {
                id = btnId
                text = symbol
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                background = createButtonDrawable(Color.parseColor("#333333"))
            }
        }
    }
}
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

object PornWatchAyarlar {
    private const val PREFS_PREFIX = "PornWatch_"
    const val ALL_CATEGORIES_ORDER_KEY = "${PREFS_PREFIX}ALL_order"

    private const val COLOR_BLACK = "#000000"
    private const val COLOR_WHITE = "#FFFFFF"

    private val defaultCategories = listOf(
        ""                            to "Latest",
        "?filter=most-viewed"         to "Most Viewed",
        "?filter=longest"             to "Longest",
        "?filter=popular"             to "Top Rated",
        "?filter=random"              to "Random Contents",
        "most-rating"                 to "Most Rating",
        "genre/appearance"                 to "Appearance",
        "genre/international"              to "International",
        "genre/ethnic"                     to "Ethnic",
        "genre/gonzo"                      to "Gonzo",
        "genre/big-tits"                   to "Big Tits",
        "genre/oral"                       to "Oral",
        "genre/age"                        to "Age",
        "genre/cumshots"                   to "Cumshots",
        "genre/anal"                       to "Anal",
        "genre/big-dicks"                  to "Big Dicks",
        "genre/amateur"                    to "Amateur",
        "genre/blowjobs"                   to "Blowjobs",
        "genre/shaved"                     to "Shaved",
        "genre/european"                   to "European",
        "genre/group-sex"                  to "Group Sex",
        "genre/clothing"                   to "Clothing",
        "genre/18-teens"                   to "18 Teens",
        "genre/interracial"                to "Interracial",
        "genre/plot-oriented"              to "Plot Oriented",
        "genre/brunettes"                  to "Brunettes",
        "genre/threesomes"                 to "Threesomes",
        "genre/character"                  to "Character",
        "genre/small-tits"                 to "Small Tits",
        "genre/sex-toy-play"               to "Sex Toy Play",
        "genre/erotic-vignette"            to "Erotic Vignette",
        "genre/facials"                    to "Facials",
        "genre/blondes"                    to "Blondes",
        "genre/big-butt"                   to "Big Butt",
        "genre/fetish"                     to "Fetish",
        "genre/naturally-busty"            to "Naturally Busty",
        "genre/popular-with-women"         to "Popular With Women",
        "genre/milf"                       to "Milf",
        "genre/masturbation"               to "Masturbation",
        "genre/compilation"                to "Compilation",
        "genre/pov"                        to "Pov",
        "genre/tattoos"                    to "Tattoos",
        "genre/lesbian"                    to "Lesbian",
        "genre/asian"                      to "Asian",
        "genre/language"                   to "Language",
        "genre/stockings"                  to "Stockings",
        "genre/pantyhose"                  to "Pantyhose",
        "genre/settings"                   to "Settings",
        "genre/bbc"                        to "Bbc",
        "genre/interracial-black-men"      to "Interracial Black Men",
        "genre/all-sex"                    to "All Sex",
        "genre/interracial-caucasian-girls" to "Interracial Caucasian Girls",
        "genre/bdsm"                       to "Bdsm",
        "genre/niche"                      to "Niche",
        "genre/creampie"                   to "Creampie",
        "genre/japanese"                   to "Japanese",
        "genre/redheads"                   to "Redheads",
        "genre/mature"                     to "Mature",
        "genre/family-roleplay"            to "Family Roleplay",
        "genre/couples"                    to "Couples",
        "genre/lingerie"                   to "Lingerie",
        "genre/feature"                    to "Feature",
        "genre/cunnilingus"                to "Cunnilingus",
        "genre/double-penetration"         to "Double Penetration",
        "genre/petite"                     to "Petite",
        "genre/black-women"                to "Black Women",
        "director/elegant-angel"              to "Elegant Angel",
        "director/new-sensations"             to "New Sensations",
        "director/digital-sin"                to "Digital Sin",
        "director/private"                    to "Private",
        "director/adam-eve"                   to "Adam & Eve",
        "director/eagle"                      to "EAGLE",
        "director/girlfriends-films"          to "Girlfriends Films",
        "director/kink-clips"                 to "Kink Clips",
        "director/lethal-hardcore"            to "Lethal Hardcore",
        "director/hot-hot-clips"              to "Hot Hot Clips",
        "director/teen-erotica-clips"         to "Teen Erotica Clips",
        "director/west-coast-productions"     to "West Coast Productions",
        "director/jonathan-jordan-xxx"        to "Jonathan Jordan XXX",
        "director/sexmex"                     to "SexMex",
        "director/grooby-clips"               to "Grooby Clips",
        "director/letsdoeit"                  to "LetsDoeIt",
        "director/hot-hot-trans"              to "Hot Hot Trans",
        "director/porn-pros"                  to "Porn Pros",
        "director/jeffs-models-clips"         to "Jeff's Models Clips",
        "director/penthouse"                  to "Penthouse",
        "director/max-a"                      to "Max-A",
        "director/dream-tranny-clips"         to "Dream Tranny Clips",
        "director/white-ghetto"               to "White Ghetto",
        "director/fullpornnetwork"            to "FullPornNetwork",
        "director/hot-video"                  to "Hot Video",
        "director/dirty-cinema"               to "Dirty Cinema",
        "director/grooby"                     to "Grooby",
        "director/team-skeet"                 to "Team Skeet",
        "director/private-society"            to "Private Society",
        "director/erotic-planet"              to "Erotic Planet",
        "director/mmv"                        to "MMV",
        "director/easy-japanese-women"        to "Easy Japanese Women",
        "director/doghouse-digital"           to "Doghouse Digital",
        "director/fun-picture"                to "Fun-Picture",
        "director/hotoldmovies"               to "HotOldmovies",
        "director/my-pervy-family"            to "My Pervy Family",
        "director/xtime"                      to "XTIME",
        "director/jm-productions"             to "JM Productions",
        "director/jpcf"                       to "JPCF",
        "director/edge-interactive"           to "Edge Interactive",
        "director/mysterious-productions"     to "Mysterious Productions",
        "director/eye-candy"                  to "Eye Candy",
        "director/pegas-productions"          to "Pegas Productions",
        "director/group-sex-jav"              to "Group Sex JAV",
        "director/adult-content-deluxe"       to "Adult Content Deluxe",
        "director/be-me-fi"                   to "be.me.fi",
        "director/taboo-heat"                 to "Taboo Heat",
        "director/jerky-girls"                to "Jerky Girls",
        "director/bb-video"                   to "BB Video",
        "director/deutschland-porno"          to "Deutschland Porno",
        "director/chickpass"                  to "ChickPass",
        "director/cx-wow"                     to "CX Wow",
        "director/combat-zone"                to "Combat Zone",
        "director/mario-salieri-productions"  to "Mario Salieri Productions",
        "director/spicylab"                   to "SpicyLab",
        "director/pornova-studios"            to "Pornova Studios",
        "director/sexwithmilfstella"          to "SexwithMilfStella",
        "director/cento-x-cento"              to "Cento X Cento",
        "director/the-flourish-xxx-theflourishxxx" to "The Flourish XXX ( TheFlourishXXX )",
        "director/aura-productions"           to "Aura Productions",
        "director/playtime-video"             to "Playtime Video",
        "director/summer-hart-studios"        to "Summer Hart Studios",
        "director/sweethearts"                to "Sweethearts",
        "director/explicit-empire"            to "Explicit Empire",
        "director/purzel"                     to "Purzel",
        "director/pure-passion"               to "Pure Passion",
        "director/jays-pov"                   to "Jay's POV",
        "director/indian-x"                   to "Indian X",
        "director/house-of-fyre"              to "House of Fyre",
        "director/hardcore-japanese"          to "Hardcore Japanese",
        "director/pornfidelity"               to "PornFidelity",
        "director/thagson"                    to "Thagson",
        "director/slim4k"                     to "Slim4K",
        "director/moviestar"                  to "Moviestar",
        "director/nasty-pixxx"                to "Nasty Pixxx",
        "director/rodney-moore-clips"         to "Rodney Moore Clips",
        "director/reality-junkies"            to "Reality Junkies",
        "director/third-world-media"          to "Third World Media",
        "director/lifeselector"               to "LifeSelector",
        "director/sexual-sin"                 to "Sexual Sin",
        "director/bang"                       to "BANG!",
        "director/kick-ass-pictures"          to "Kick Ass Pictures",
        "director/immoral-productions-clips"  to "Immoral Productions Clips",
        "director/onlytaboo"                  to "OnlyTaboo",
        "director/tadpolexxxstudio-clips"     to "TadpoleXXXStudio Clips",
        "director/adptube"                    to "ADPTube",
        "director/clarkes-boutaine-productions" to "Clarkes Boutaine Productions",
        "director/maiko-pictures-clips"       to "Maiko Pictures Clips",
        "director/julia-reaves"               to "Julia Reaves",
        "director/muschi-movie"               to "Muschi Movie",
        "release-year/2026"                   to "2026",
        "release-year/2025"                   to "2025",
        "release-year/2024"                   to "2024",
        "release-year/2023"                   to "2023",
        "release-year/2022"                   to "2022",
        "release-year/2021"                   to "2021",
        "release-year/2020"                   to "2020",
        "release-year/2019"                   to "2019",
        "release-year/2018"                   to "2018",
        "release-year/2017"                   to "2017",
        "release-year/2016"                   to "2016",
        "release-year/2015"                   to "2015",
        "release-year/2014"                   to "2014",
        "release-year/2013"                   to "2013",
        "release-year/2012"                   to "2012",
        "release-year/2011"                   to "2011",
        "release-year/2010"                   to "2010",
        "release-year/2009"                   to "2009",
        "release-year/2008"                   to "2008",
        "release-year/2007"                   to "2007",
        "release-year/2006"                   to "2006",
        "release-year/2005"                   to "2005",
        "release-year/2004"                   to "2004",
        "release-year/2003"                   to "2003",
        "release-year/2002"                   to "2002",
        "release-year/2001"                   to "2001",
        "release-year/2000"                   to "2000",
        "release-year/1999"                   to "1999",
        "release-year/1998"                   to "1998",
        "release-year/1997"                   to "1997",
        "release-year/1996"                   to "1996",
        "release-year/1995"                   to "1995",
        "release-year/1994"                   to "1994",
        "release-year/1993"                   to "1993",
        "release-year/1989"                   to "1989"
    )

    private val defaultEnabledNames = setOf(
        "Latest", "Most Viewed", "Random Contents"
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

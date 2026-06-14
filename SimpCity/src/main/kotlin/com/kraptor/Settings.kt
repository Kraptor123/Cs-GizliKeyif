package com.kraptor

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

object Settings {
    const val PREFS_NAME = "simpcity_settings"
    const val KEY_USER = "username"
    const val KEY_PASS = "password"
    const val KEY_PAGES = "pages_to_load"

    const val DEFAULT_USER = "ksbkb51102"
    const val DEFAULT_PASS = "ksbkb51102"
    const val DEFAULT_PAGES = "5"

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER, DEFAULT_USER).let { if (it.isNullOrBlank()) DEFAULT_USER else it }
    }

    fun getPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PASS, DEFAULT_PASS).let { if (it.isNullOrBlank()) DEFAULT_PASS else it }
    }

    fun getPages(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_PAGES, DEFAULT_PAGES)
        return value?.toIntOrNull() ?: DEFAULT_PAGES.toInt()
    }
}

class SettingsDialog(val activity: AppCompatActivity, val onSave: () -> Unit) {

    private val prefs by lazy { activity.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE) }
    private val COLOR_BG = "#1A1C1E"
    private val COLOR_ACCENT = "#FFD600"
    private val fontBold = Typeface.create("sans-serif", Typeface.BOLD)

    private fun dpToPx(dp: Int): Int = (dp * activity.resources.displayMetrics.density).toInt()

    private fun createRoundedDrawable(color: String, radius: Float = 12f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(radius.toInt()).toFloat()
            setColor(Color.parseColor(color))
        }
    }

    private fun createEditText(hint: String, current: String, isNumber: Boolean = false): EditText {
        return EditText(activity).apply {
            setHint(hint)
            setText(current)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = createRoundedDrawable("#2A2C2E", 8f)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            textSize = 14f
            isFocusable = true
            isFocusableInTouchMode = true
            if (isNumber) {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            setOnFocusChangeListener { v, hasFocus ->
                (v.background as? GradientDrawable)?.setStroke(if (hasFocus) dpToPx(2) else 0, Color.WHITE)
            }
        }
    }

    fun show(mainUrl: String? = null) {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedDrawable(COLOR_BG, 24f)
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
            layoutParams = LinearLayout.LayoutParams(dpToPx(380), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        root.addView(TextView(activity).apply {
            text = "SimpCity Settings"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = fontBold
            setPadding(0, 0, 0, dpToPx(20))
        })

        // Username Field
        root.addView(TextView(activity).apply { text = "User Name"; setTextColor(Color.GRAY); textSize = 11f })
        val userEdit = createEditText("User Name", Settings.getUsername(activity))
        root.addView(userEdit, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dpToPx(12) })

        // Password Field
        root.addView(TextView(activity).apply { text = "Password"; setTextColor(Color.GRAY); textSize = 11f })
        val passEdit = createEditText("Password", Settings.getPassword(activity)).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(passEdit, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dpToPx(12) })

        // Pages Field
        root.addView(TextView(activity).apply { text = "Pages To Load"; setTextColor(Color.GRAY); textSize = 11f })
        val pagesEdit = createEditText("Örn: 5", Settings.getPages(activity).toString(), true)
        root.addView(pagesEdit, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dpToPx(24) })

        // Action Buttons
        val btnLogout = Button(activity).apply {
            text = "Log Out (Delete Cookies)"
            setTextColor(Color.parseColor("#FF8989"))
            background = createRoundedDrawable("#331A1A", 12f)
            applyTvEffect(this)
        }

        val btnSave = Button(activity).apply {
            text = "Save"
            setTextColor(Color.BLACK)
            background = createRoundedDrawable(COLOR_ACCENT, 12f)
            applyTvEffect(this)
        }

        root.addView(btnLogout, LinearLayout.LayoutParams(-1, dpToPx(48)).apply { bottomMargin = dpToPx(12) })
        root.addView(btnSave, LinearLayout.LayoutParams(-1, dpToPx(48)))

        val dialog = AlertDialog.Builder(activity).setView(root).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

        btnLogout.setOnClickListener {
            clearSimpCookie(mainUrl)
            Toast.makeText(activity, "Cookies cleared and logged out.", Toast.LENGTH_SHORT).show()
            com.lagradost.cloudstream3.MainActivity.reloadHomeEvent.invoke(true)
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putString(Settings.KEY_USER, userEdit.text.toString())
                .putString(Settings.KEY_PASS, passEdit.text.toString())
                .putString(Settings.KEY_PAGES, pagesEdit.text.toString())
                .apply()
            onSave()
            Toast.makeText(activity, "Changes saved.", Toast.LENGTH_SHORT).show()
            com.lagradost.cloudstream3.MainActivity.reloadHomeEvent.invoke(true)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyTvEffect(view: View) {
        view.isFocusable = true
        view.setOnFocusChangeListener { v, hasFocus ->
            if (v.background is GradientDrawable) {
                val gd = v.background as GradientDrawable
                if (hasFocus) {
                    gd.setStroke(dpToPx(3), Color.WHITE)
                    v.scaleX = 1.05f; v.scaleY = 1.05f
                } else {
                    gd.setStroke(0, Color.TRANSPARENT)
                    v.scaleX = 1f; v.scaleY = 1f
                }
            }
        }
    }
}

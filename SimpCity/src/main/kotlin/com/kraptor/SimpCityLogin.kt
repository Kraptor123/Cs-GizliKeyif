package com.kraptor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private var simpActiveDialog: AlertDialog? = null
private val simpWaitingList = mutableListOf<Continuation<String>>()
private var simpIsProcessing = false

fun getSimpCookie(): String {
    return SimpCityPlugin.appContext.getSharedPreferences("simp_cookies", Context.MODE_PRIVATE)
        .getString("simpcity.cr", "") ?: ""
}

fun clearSimpCookie() {
    SimpCityPlugin.appContext.getSharedPreferences("simp_cookies", Context.MODE_PRIVATE)
        .edit().remove("simpcity.cr").apply()
}

@SuppressLint("SetJavaScriptEnabled")
suspend fun simpLogin(username: String, password: String, forceRefresh: Boolean = false): String {
    val context = SimpCityPlugin.appContext
    val saved = getSimpCookie()
    if (!forceRefresh && saved.isNotEmpty() && saved.contains("_user=")) {
        return saved
    }

    if (simpIsProcessing) {
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { simpWaitingList.remove(cont) }
            simpWaitingList.add(cont)
        }
    }

    simpIsProcessing = true

    val initialUserCookie = (CookieManager.getInstance().getCookie("https://simpcity.cr") ?: "")
        .split(";")
        .map { it.trim() }
        .find { it.contains("_user=") } ?: ""

    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            simpWaitingList.remove(continuation)
            if (simpWaitingList.isEmpty()) simpIsProcessing = false
        }

        MainScope().launch {
            val prefs = context.getSharedPreferences("simp_cookies", Context.MODE_PRIVATE)
            var resumed = false

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(Color.TRANSPARENT)
            }

            val webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(Color.TRANSPARENT)
            }
            container.addView(webView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ))

            fun safeResumeAll(value: String) {
                if (!resumed) {
                    resumed = true
                    runCatching { continuation.resume(value) }
                }
                val copy = simpWaitingList.toList()
                simpWaitingList.clear()
                copy.forEach { runCatching { it.resume(value) } }
                simpIsProcessing = false
            }

            val safeUser = username.replace("\\", "\\\\").replace("'", "\\'")
            val safePass = password.replace("\\", "\\\\").replace("'", "\\'")

            val helperJs = """
                (function() {
                    var u = document.querySelector('input[name="login"]');
                    var p = document.querySelector('input[name="password"]');
                    if (u && !u.value) {
                        u.value = '$safeUser';
                        u.dispatchEvent(new Event('input', {bubbles: true}));
                        u.dispatchEvent(new Event('change', {bubbles: true}));
                    }
                    if (p && !p.value) {
                        p.value = '$safePass';
                        p.dispatchEvent(new Event('input', {bubbles: true}));
                        p.dispatchEvent(new Event('change', {bubbles: true}));
                    }

                    var s = document.createElement('style');
                    s.textContent = `
                        *:focus { outline: 3px solid #FFD600 !important; }
                        .input:focus { border-color: #FFD600 !important; }
                        .button--primary { min-height: 48px !important; font-size: 18px !important; }
                        .button--primary:focus { box-shadow: 0 0 15px #FFD600 !important; }

                        [data-captcha-widget] img,
                        [data-captcha-widget] [style*="background-image"],
                        [data-captcha-widget] .selti-item,
                        [data-captcha-widget] .captcha-item,
                        [data-captcha-widget] .captcha-grid-item {
                            border: 3px solid transparent !important;
                            transition: all 0.15s !important;
                            cursor: pointer !important;
                            margin: 4px !important;
                            display: inline-block !important;
                        }
                        [data-captcha-widget] img:focus,
                        [data-captcha-widget] [style*="background-image"]:focus,
                        [data-captcha-widget] .selti-item:focus,
                        [data-captcha-widget] .captcha-item:focus,
                        [data-captcha-widget] .captcha-grid-item:focus {
                            border-color: #FFD600 !important;
                            transform: scale(1.12) !important;
                            box-shadow: 0 0 20px #FFD600 !important;
                            outline: none !important;
                        }

                        .formRow-label { color: #ddd !important; }
                    `;
                    document.head.appendChild(s);

                    function makeFocusable() {
                        var w = document.querySelector('[data-captcha-widget]');
                        if (!w) return;
                        w.querySelectorAll('img, [style*="background-image"], .selti-item, .captcha-item, .captcha-grid-item').forEach(function(el) {
                            if (!el.getAttribute('tabindex')) {
                                el.setAttribute('tabindex', '0');
                            }
                        });
                    }

                    var obs = new MutationObserver(makeFocusable);
                    obs.observe(document.body, {childList: true, subtree: true});
                    setInterval(makeFocusable, 800);

                    document.addEventListener('keydown', function(e) {
                        if ((e.key === 'Enter' || e.keyCode === 13) && document.activeElement) {
                            var w = document.querySelector('[data-captcha-widget]');
                            if (w && w.contains(document.activeElement)) {
                                document.activeElement.click();
                                e.preventDefault();
                            }
                        }
                    });
                })();
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url?.contains("/login") == true) {
                        view?.evaluateJavascript(helperJs, null)
                    }
                }
            }

            webView.loadUrl("https://simpcity.cr/login/")

            simpActiveDialog = AlertDialog.Builder(context)
                .setView(container)
                .setCancelable(false)
                .setNegativeButton("İptal") { _, _ -> safeResumeAll("") }
                .create()

            simpActiveDialog?.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0.9f)
            }

            simpActiveDialog?.setOnShowListener { webView.requestFocus() }
            simpActiveDialog?.setOnDismissListener {
                if (!resumed) safeResumeAll("")
                simpActiveDialog = null
            }

            MainScope().launch {
                while (simpActiveDialog?.isShowing == true) {
                    delay(1500)

                    val currentCookie = CookieManager.getInstance().getCookie("https://simpcity.cr") ?: ""
                    val currentUserCookie = currentCookie.split(";")
                        .map { it.trim() }
                        .find { it.contains("_user=") } ?: ""

                    if (currentUserCookie.isNotEmpty() && currentUserCookie != initialUserCookie) {
                        prefs.edit().putString("simpcity.cr", currentCookie).apply()
                        webView.evaluateJavascript(
                            "document.body.innerHTML='<div style=\"display:flex;align-items:center;justify-content:center;height:100vh\"><h1 style=\"color:#00ff00;font-size:42px;font-family:sans-serif\">✅ Giriş Başarılı!</h1></div>'",
                            null
                        )
                        delay(1200)
                        simpActiveDialog?.dismiss()
                        safeResumeAll(currentCookie)
                        return@launch
                    }
                }
            }

            simpActiveDialog?.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            simpActiveDialog?.dismiss()
                            safeResumeAll("")
                            true
                        }
                        else -> false
                    }
                } else false
            }

            simpActiveDialog?.show()
        }
    }
}

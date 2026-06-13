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
                setPadding(0, 0, 0, 0)
                setBackgroundColor(Color.BLACK)
            }

            val webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(Color.BLACK)
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
                    var isTv = window.innerWidth <= 1280 || navigator.userAgent.toLowerCase().indexOf('tv') > -1;
                    
                    var style = document.createElement('style');
                    style.textContent = `
                        *:focus { outline: 4px solid #FFD600 !important; outline-offset: 2px !important; }
                        .input:focus { border-color: #FFD600 !important; box-shadow: 0 0 10px #FFD600 !important; }
                        
                        /* Captcha Challenge Container Scaling for TV */
                        div[style*="z-index"][style*="2147483647"], 
                        div[class*="captcha-challenge"],
                        #captcha-challenge,
                        .h-captcha-challenge {
                            transform: scale(0.85) !important;
                            transform-origin: center top !important;
                            max-height: 95vh !important;
                            top: 10px !important;
                        }

                        /* Make verification items obvious */
                        [data-captcha-widget] img, 
                        .captcha-item, .selti-item, .challenge-image {
                            cursor: pointer !important;
                            border: 3px solid transparent !important;
                        }
                        
                        [data-captcha-widget] img:focus,
                        .captcha-item:focus, .selti-item:focus, .challenge-image:focus {
                            border-color: #FFD600 !important;
                            transform: scale(1.05) !important;
                        }

                        /* Verify / Submit Button */
                        .button--primary, button[type="submit"], .verify-button, .button-submit {
                            min-height: 50px !important;
                            font-weight: bold !important;
                        }
                    `;
                    document.head.appendChild(style);

                    function setupAutomation() {
                        var u = document.querySelector('input[name="login"]');
                        var p = document.querySelector('input[name="password"]');
                        if (u && !u.value) { u.value = '$safeUser'; u.dispatchEvent(new Event('change', {bubbles:true})); }
                        if (p && !p.value) { p.value = '$safePass'; p.dispatchEvent(new Event('change', {bubbles:true})); }
                    }

                    function makeEverythingFocusable() {
                        var elements = document.querySelectorAll('input, button, a, [role="button"], [data-captcha-widget] img, .captcha-item, .selti-item, [tabindex]');
                        elements.forEach(function(el) {
                            if (!el.getAttribute('tabindex')) {
                                el.setAttribute('tabindex', '0');
                            }
                        });

                        // If a challenge is visible, try to move focus inside it
                        var challenge = document.querySelector('div[style*="z-index"][style*="2147483647"]');
                        if (challenge && !challenge.contains(document.activeElement)) {
                            var first = challenge.querySelector('input, button, [tabindex="0"]');
                            if (first) first.focus();
                        }
                    }

                    setInterval(function() {
                        setupAutomation();
                        makeEverythingFocusable();
                    }, 1000);

                    document.addEventListener('keydown', function(e) {
                        if ((e.key === 'Enter' || e.keyCode === 13) && document.activeElement) {
                            if (document.activeElement.tagName !== 'INPUT' || document.activeElement.type === 'checkbox') {
                                document.activeElement.click();
                            }
                        }
                    });
                })();
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(helperJs, null)
                }
            }

            webView.loadUrl("https://simpcity.cr/login/")

            simpActiveDialog = AlertDialog.Builder(context)
                .setView(container)
                .setCancelable(false)
                .setNegativeButton("İptal") { _, _ -> safeResumeAll("") }
                .create()

            simpActiveDialog?.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.BLACK))
                setDimAmount(1.0f)
                setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            }

            simpActiveDialog?.setOnShowListener { webView.requestFocus() }
            simpActiveDialog?.setOnDismissListener {
                if (!resumed) safeResumeAll("")
                simpActiveDialog = null
            }

            MainScope().launch {
                while (simpActiveDialog?.isShowing == true) {
                    delay(2000)
                    val currentCookie = CookieManager.getInstance().getCookie("https://simpcity.cr") ?: ""
                    if (currentCookie.contains("_user=") && currentCookie != initialUserCookie) {
                        prefs.edit().putString("simpcity.cr", currentCookie).apply()
                        webView.evaluateJavascript("document.body.innerHTML='<h1 style=\"color:white;text-align:center;margin-top:20%\">Giriş Başarılı!</h1>'", null)
                        delay(1000)
                        simpActiveDialog?.dismiss()
                        safeResumeAll(currentCookie)
                        return@launch
                    }
                }
            }

            simpActiveDialog?.show()
        }
    }
}

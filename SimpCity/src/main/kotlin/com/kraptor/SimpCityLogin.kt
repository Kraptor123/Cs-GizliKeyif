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

fun clearSimpCookie(baseUrl: String? = null) {
    SimpCityPlugin.appContext.getSharedPreferences("simp_cookies", Context.MODE_PRIVATE)
        .edit().remove("simpcity.cr").apply()

    val cookieManager = CookieManager.getInstance()
    val url = baseUrl ?: "https://simpcity.cr"
    val cookies = cookieManager.getCookie(url)
    if (cookies != null) {
        val cookiePairs = cookies.split(";")
        for (pair in cookiePairs) {
            val name = pair.split("=")[0].trim()
            cookieManager.setCookie(url, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
        }
        cookieManager.flush()
    }
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
                settings.setSupportMultipleWindows(false)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setNeedInitialFocus(true)
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(Color.BLACK)

                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                evaluateJavascript("""
                                    (function() {
                                        var el = document.activeElement;
                                        if (!el) return;
                                        if (el.tagName === 'IFRAME') return; // Let default handling take over for iframes
                                        
                                        var opts = { bubbles: true, cancelable: true, view: window };
                                        el.dispatchEvent(new MouseEvent('mousedown', opts));
                                        el.dispatchEvent(new MouseEvent('mouseup', opts));
                                        el.dispatchEvent(new MouseEvent('click', opts));
                                    })();
                                """.trimIndent(), null)
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
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
                    var style = document.createElement('style');
                    style.textContent = `
                        *:focus { outline: 4px solid #FFD600 !important; outline-offset: 2px !important; box-shadow: 0 0 15px #FFD600 !important; }
                        input:focus, button:focus, a:focus, [tabindex]:focus { border-color: #FFD600 !important; }
                        
                        /* Overlay & Captcha Styling for TV */
                        .captcha-widget, .captcha-container, .captcha-popup, .captcha-window,
                        div[style*="z-index"][style*="2147483647"], 
                        div[class*="captcha-challenge"],
                        #captcha-challenge,
                        .h-captcha-challenge,
                        iframe[src*="turnstile"],
                        iframe[src*="hcaptcha"],
                        iframe[src*="recaptcha"] {
                            transform: scale(0.9) !important;
                            transform-origin: center center !important;
                            max-height: 95vh !important;
                            position: fixed !important;
                            top: 50% !important;
                            left: 50% !important;
                            translate: -50% -50% !important;
                            z-index: 2147483647 !important;
                            background: #111 !important;
                            border: 2px solid #333 !important;
                            box-shadow: 0 0 50px rgba(0,0,0,0.9) !important;
                            pointer-events: auto !important;
                        }

                        /* Ensure background is non-interactable when captcha is active */
                        .captcha-active-trap {
                            pointer-events: none !important;
                            filter: blur(2px) grayscale(0.5) !important;
                        }

                        /* Ensure iframes are visible when focused */
                        iframe:focus {
                            border: 4px solid #FFD600 !important;
                        }

                        [data-captcha-widget] img, .captcha-item, .selti-item, .challenge-image {
                            cursor: pointer !important;
                            border: 3px solid transparent !important;
                        }
                        
                        [data-captcha-widget] img:focus, .captcha-item:focus, .selti-item:focus, .challenge-image:focus {
                            border-color: #FFD600 !important;
                            transform: scale(1.05) !important;
                        }

                        .button--primary, button[type="submit"], .verify-button, .button-submit {
                            min-height: 55px !important;
                            font-weight: bold !important;
                            font-size: 1.1em !important;
                        }
                    `;
                    document.head.appendChild(style);

                    function simulateClick(el) {
                        if (!el) return;
                        var opts = { bubbles: true, cancelable: true, view: window };
                        el.dispatchEvent(new MouseEvent('mousedown', opts));
                        el.dispatchEvent(new MouseEvent('mouseup', opts));
                        el.dispatchEvent(new MouseEvent('click', opts));
                    }

                    function setupAutomation() {
                        var u = document.querySelector('input[name="login"]');
                        var p = document.querySelector('input[name="password"]');
                        if (u && !u.value) { u.value = '$safeUser'; u.dispatchEvent(new Event('input', {bubbles:true})); }
                        if (p && !p.value) { p.value = '$safePass'; p.dispatchEvent(new Event('input', {bubbles:true})); }
                    }

                    var captchaSelectors = [
                        '.captcha-widget', '.captcha-container', '.captcha-popup', '.captcha-window',
                        'div[style*="z-index"][style*="2147483647"]',
                        'iframe[src*="turnstile"]',
                        'iframe[src*="hcaptcha"]',
                        '.h-captcha-challenge'
                    ];

                    function isCaptchaVisible() {
                        for (var selector of captchaSelectors) {
                            var el = document.querySelector(selector);
                            if (el && el.offsetHeight > 0) return el;
                        }
                        return null;
                    }

                    function makeEverythingFocusable() {
                        var captcha = isCaptchaVisible();
                        var container = captcha || document;

                        var elements = container.querySelectorAll('input, button, a, [role="button"], [data-captcha-widget] img, .captcha-item, .selti-item, [tabindex], iframe');
                        elements.forEach(function(el) {
                            if (!el.getAttribute('tabindex')) {
                                el.setAttribute('tabindex', '0');
                            }
                        });

                        if (captcha) {
                            // Focus Trap: Disable focus on everything outside captcha
                            document.querySelectorAll('body > *:not(.captcha-trap-ignore)').forEach(function(el) {
                                if (!el.contains(captcha)) {
                                    el.classList.add('captcha-active-trap');
                                    el.setAttribute('aria-hidden', 'true');
                                }
                            });
                            captcha.classList.add('captcha-trap-ignore');
                            
                            if (!captcha.contains(document.activeElement)) {
                                var first = captcha.querySelector('input, button, [tabindex="0"], iframe');
                                if (first) first.focus();
                                else captcha.focus();
                            }
                        } else {
                            // Cleanup trap if no captcha
                            document.querySelectorAll('.captcha-active-trap').forEach(function(el) {
                                el.classList.remove('captcha-active-trap');
                                el.removeAttribute('aria-hidden');
                            });
                        }
                    }

                    // Watch for dynamic captcha creation
                    var observer = new MutationObserver(function(mutations) {
                        var found = false;
                        mutations.forEach(function(mutation) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    for (var cls of ['captcha-widget', 'captcha-container', 'captcha-popup', 'captcha-window']) {
                                        if (node.classList.contains(cls) || node.querySelector('.' + cls)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                            });
                        });
                        if (found) makeEverythingFocusable();
                    });
                    observer.observe(document.body, { childList: true, subtree: true });

                    setInterval(function() {
                        setupAutomation();
                        makeEverythingFocusable();
                    }, 1000);

                    // Help DPAD enter iframes
                    document.addEventListener('focusin', function(e) {
                        if (e.target.tagName === 'IFRAME') {
                            // Some TV browsers need a click to activate iframe spatial navigation
                            simulateClick(e.target);
                        }
                    }, true);

                    document.addEventListener('keydown', function(e) {
                        if ((e.key === 'Enter' || e.keyCode === 13) && document.activeElement) {
                            var el = document.activeElement;
                            if (el.tagName === 'IFRAME') {
                                // Let standard browser behavior handle Enter for iframes
                            } else if (el.tagName !== 'INPUT' || el.type === 'checkbox' || el.type === 'submit') {
                                simulateClick(el);
                                e.preventDefault();
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

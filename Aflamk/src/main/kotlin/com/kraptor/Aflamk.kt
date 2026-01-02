// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Aflamk(context: Context) : MainAPI() {
    override var mainUrl              = "https://www.aflamk1.net"
    override var name                 = "Aflamk"
    override val hasMainPage          = true
    override var lang                 = "ar"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    private  val context = context

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/سكس-مترجم/"            to "سكس مترجم",
        "${mainUrl}/categories/سكس-محارم-مترجم/"      to "سكس محارم",
        "${mainUrl}/categories/سكس-اخوات-مترجم/"      to "سكس اخوات",
        "${mainUrl}/categories/سكس-امهات-مترجم/"      to "سكس امهات",
        "${mainUrl}/categories/سكس-جماعى-مترجم/"      to "سكس جماعى",
        "${mainUrl}/categories/سكس-محارم-الاب-مترجم/"  to "سكس محارم الاب",
        "${mainUrl}/categories/سكس-امهات-مترجم/"      to "سكس امهات مترجم",
        "${mainUrl}/categories/سكس-سحاق-مترجم/"       to "سكس سحاق مترجم",
        "${mainUrl}/categories/سكس-محارم-مترجم/"      to "سكس محارم مترجم",
        "${mainUrl}/categories/سكس-مترجم/"            to "سكس مترجم",
        "${mainUrl}/categories/سكس-محارم-الاب-مترجم/"  to "سكس محارم الاب مترجم",
        "${mainUrl}/categories/سكس-اخوات-مترجم/"      to "سكس اخوات مترجم",
        "${mainUrl}/categories/سكس-جماعى-مترجم/"      to "سكس جماعى مترجم",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=post_date&from=$page").document
        val home     = document.select("div.list-videos div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("strong.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-original"))
        val rating    = this.selectFirst("div.rating")?.text()?.replace("%","")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.score     = Score.from100(rating)
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/?q=$query&mode=async&function=get_block&block_id=list_videos_videos_list_search_result&category_ids=&sort_by=&from_videos=$page&from_albums=$page").document

        val aramaCevap = document.select("div.list-videos div.item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val istek = app.get(url)
        val document = istek.document
        val cookies = istek.cookies.toString()
        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = document.selectFirst("div.item:contains(لوصف:) em")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.item:contains(كلمات البحث) a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val durationText = document.selectFirst("div.item:contains(المدة) em")
            ?.text()
            ?.trim()
        val parts = durationText?.split(":")?.map { it.toIntOrNull() }
        val totalMinutes = when (parts?.size) {
            3 -> parts[0]!! * 60 + parts[1]!!
            2 -> parts[0]!!
            1 -> parts[0]!!
            else -> null
        }

        val recommendations = document.select("div.list-videos div.item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.item:contains(لتصنيفات:) a").map { Actor(it.text()) }
        val trailer         = Regex("""embed\/(.*)\?rel""").find(document.html())?.groupValues?.get(1)?.let { "https://www.youtube.com/embed/$it" }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.posterHeaders   = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
                "Referer" to "${mainUrl}/",
                "cookies" to cookies
            )
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(score)
            this.duration        = totalMinutes
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractVideoUrls(
        context: Context,
        html: String
    ): List<String> = suspendCoroutine { continuation ->

        Handler(Looper.getMainLooper()).post {
            val wv = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        // JavaScript'in çalışması için bekle
                        Handler(Looper.getMainLooper()).postDelayed({
                            view?.evaluateJavascript("""
                            (function() {
                                var videos = [];
                                
                                // flashvars objesinden videoları al
                                if (typeof flashvars !== 'undefined') {
                                    // video_url (720p)
                                    if (flashvars.video_url && flashvars.video_url.indexOf('https://') !== -1) {
                                        videos.push(flashvars.video_url);
                                    }
                                    // video_alt_url (1080p)
                                    if (flashvars.video_alt_url && flashvars.video_alt_url.indexOf('https://') !== -1) {
                                        videos.push(flashvars.video_alt_url);
                                    }
                                }
                                
                                // Eğer flashvars'dan bulamadıysa, script'leri tara
                                if (videos.length === 0) {
                                    var scripts = document.getElementsByTagName('script');
                                    for (var i = 0; i < scripts.length; i++) {
                                        var text = scripts[i].textContent;
                                        
                                        // https://www.aflamk1.net/get_file/ ile başlayan .mp4 linklerini bul
                                        var matches = text.match(/https:\/\/aflamk1\.net\/get_file\/[^\s'"]+\.mp4[^\s'"]*/g);
                                        
                                        if (matches) {
                                            for (var j = 0; j < matches.length; j++) {
                                                videos.push(matches[j]);
                                            }
                                        }
                                    }
                                }
                                
                                // Tekrarları temizle
                                videos = videos.filter(function(item, pos) {
                                    return videos.indexOf(item) === pos;
                                });
                                
                                return JSON.stringify(videos);
                            })();
                        """) { result ->
                                try {
                                    val cleanResult = result.trim('"').replace("\\", "")
                                    val videoUrls = JSONArray(cleanResult)

                                    val urls = mutableListOf<String>()
                                    for (i in 0 until videoUrls.length()) {
                                        urls.add(videoUrls.getString(i))
                                    }

                                    continuation.resume(urls)

                                    Handler(Looper.getMainLooper()).post {
                                        try {
                                            this@apply.stopLoading()
                                            this@apply.clearHistory()
                                            this@apply.destroy()
                                        } catch (ignored: Throwable) {}
                                    }
                                } catch (e: Exception) {
                                    Log.e("VideoExtractor", "Error: ${e.message}")
                                    continuation.resume(emptyList())
                                }
                            }
                        }, 100)
                    }
                }

                loadDataWithBaseURL("https://www.aflamk1.net", html, "text/html", "UTF-8", null)
            }
        }
    }


    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val videoUrls = extractVideoUrls(context, document)

        videoUrls.forEach { url ->
            Log.d("kraptor_$name", "url = ${url}")
            callback.invoke(
                newExtractorLink(
                    name,
                    name,
                    url,
                    type = ExtractorLinkType.VIDEO,
                    {
                        this.referer = "$mainUrl/"
                    }
                )
            )
        }

        return videoUrls.isNotEmpty()
    }
}
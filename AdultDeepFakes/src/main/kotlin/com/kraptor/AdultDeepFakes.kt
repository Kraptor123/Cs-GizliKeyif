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
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AdultDeepFakes(context: Context) : MainAPI() {
    override var mainUrl              = "https://adultdeepfakes.com"
    override var name                 = "AdultDeepFakes"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded
    private var context = context

    override val mainPage = mainPageOf(
        "${mainUrl}/top-rated/"                                    to "Top Rated",
        "${mainUrl}/tags/passionate/"                              to "passionate",
        "${mainUrl}/tags/creampie/"                                to "creampie",
        "${mainUrl}/tags/fetish/"                                  to "fetish",
        "${mainUrl}/tags/nsfw/"                                    to "nsfw",
        "${mainUrl}/tags/foot/"                                    to "foot",
        "${mainUrl}/tags/sex/"                                     to "sex",
        "${mainUrl}/tags/missionary/"                              to "missionary",
        "${mainUrl}/tags/2-naked/"                                 to "naked",
        "${mainUrl}/tags/model/"                                   to "model",
        "${mainUrl}/tags/feet/"                                    to "feet",
        "${mainUrl}/tags/deepfake/"                                to "deepfake",
        "${mainUrl}/tags/porn/"                                    to "porn",
        "${mainUrl}/tags/fashion-model/"                           to "fashion",
        "${mainUrl}/tags/asian/"                                   to "asian",
        "${mainUrl}/tags/stockings/"                               to "stockings",
        "${mainUrl}/tags/deepfakekpop/"                            to "deepfakekpop",
        "${mainUrl}/tags/young/"                                   to "young",
        "${mainUrl}/tags/facial/"                                  to "facial",
        "${mainUrl}/tags/kpopdeepfake/"                            to "kpopdeepfake",
        "${mainUrl}/tags/pop-idol/"                                to "pop",
        "${mainUrl}/tags/fc323ac10f06036b367374c409769002/"        to "딥페이크",
        "${mainUrl}/tags/kpop/"                                    to "kpop",
        "${mainUrl}/tags/jidol/"                                   to "jidol",
        "${mainUrl}/tags/solo/"                                    to "solo",
        "${mainUrl}/tags/jakefakes/"                               to "jakefakes",
        "${mainUrl}/tags/hot/"                                     to "hot",
        "${mainUrl}/tags/american/"                                to "american",
        "${mainUrl}/tags/nogizaka/"                                to "nogizaka",
        "${mainUrl}/tags/straight-sex/"                            to "straight",
        "${mainUrl}/tags/nude/"                                    to "nude",
        "${mainUrl}/tags/scene/"                                   to "scene",
        "${mainUrl}/tags/81648136f80da41f036112879cfa5be1/"        to "乃木坂",
        "${mainUrl}/tags/feet-fetish/"                             to "feet",
        "${mainUrl}/tags/censored/"                                to "censored",
        "${mainUrl}/tags/avengers/"                                to "avengers",
        "${mainUrl}/tags/foot-fetish/"                             to "foot",
        "${mainUrl}/tags/fake/"                                    to "fake",
        "${mainUrl}/tags/korean/"                                  to "korean",
        "${mainUrl}/tags/blacked/"                                 to "blacked",
        "${mainUrl}/tags/actress/"                                 to "actress",
        "${mainUrl}/tags/bbc/"                                     to "bbc",
        "${mainUrl}/tags/50e81d7d3ded1e1b770419be096d3adc/"        to "김태연",
        "${mainUrl}/tags/indian/"                                  to "indian",
        "${mainUrl}/tags/46/"                                      to "乃木坂46",
        "${mainUrl}/tags/blowjob/"                                 to "blowjob",
        "${mainUrl}/tags/blonde/"                                  to "blonde",
        "${mainUrl}/tags/pov/"                                     to "pov",
        "${mainUrl}/tags/hardcore/"                                to "hardcore",
        "${mainUrl}/tags/uncensored/"                              to "uncensored",
        "${mainUrl}/tags/cumshot/"                                 to "cumshot",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?mode=async&function=get_block&block_id=list_videos_common_videos_list&sort_by=rating&items_per_page=24&from=$page", headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "X-Requested-With" to "XMLHttpRequest",
            "Connection" to "keep-alive",
            "Referer" to "${mainUrl}/",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-origin",
            "Priority" to "u=0",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )).document

        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("strong.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))
//        val trailer   = fixUrlNull(this.selectFirst("img")?.attr("data-preview"))
        val private   = this.selectFirst("span.ico-private")?.text() ?: ""

        if (private.contains("private",true)){
            return null
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/${query}/?mode=async&function=get_block&block_id=list_videos_videos_list_search_result&q=$query").document

        val aramaCevap = document.select("div.item").mapNotNull { it.toMainPageResult() }
        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("link[rel=preload]")?.attr("href"))
        val description     = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.info div.item:containsOwn(tags) a , div.info div.item:containsOwn(categories) a").map { it.text() }
        val score          = document.selectFirst("span.voters")?.text()?.split(" ")[0]?.replace("%","")?.trim()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.item").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("div.info div.item:containsOwn(celebrities) a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from100(score)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
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
                                
                                // URL temizleme fonksiyonu (function/0/... vb. kalıntıları atar)
                                var cleanUrl = function(urlStr) {
                                    if (!urlStr) return null;
                                    // https:// ile başlayıp .mp4 ile biten kısmı alır
                                    var match = urlStr.match(/(https:\/\/[^'"\s]+\.mp4)/);
                                    return match ? match[1] : null;
                                };

                                if (typeof flashvars !== 'undefined') {
                                    // 1. video_url (Genelde 480p veya SD)
                                    var url1 = cleanUrl(flashvars.video_url);
                                    if (url1) {
                                        videos.push({
                                            url: url1,
                                            quality: flashvars.video_url_text || 'SD'
                                        });
                                    }
                                    
                                    // 2. video_alt_url (Genelde 720p veya HD)
                                    var url2 = cleanUrl(flashvars.video_alt_url);
                                    if (url2) {
                                        videos.push({
                                            url: url2,
                                            quality: flashvars.video_alt_url_text || 'HD'
                                        });
                                    }
                                }
                                
                                // Eğer flashvars'dan bulamadıysa, script'leri tara (Fallback)
                                if (videos.length === 0) {
                                    var scripts = document.getElementsByTagName('script');
                                    for (var i = 0; i < scripts.length; i++) {
                                        var text = scripts[i].textContent;
                                        var matches = text.match(/https:\/\/adultdeepfakes\.com\/get_file\/[^\s'"]+\.mp4[^\s'"]*/g);
                                        
                                        if (matches) {
                                            for (var j = 0; j < matches.length; j++) {
                                                var cleanedMatch = cleanUrl(matches[j]);
                                                if (cleanedMatch) {
                                                    videos.push({
                                                        url: cleanedMatch,
                                                        quality: 'Unknown' // Script içinde kalite yazmıyorsa varsayılan
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Tekrarları temizle (URL'ye göre)
                                var uniqueVideos = [];
                                var seen = {};
                                for (var k = 0; k < videos.length; k++) {
                                    var item = videos[k];
                                    if (!seen[item.url]) {
                                        seen[item.url] = true;
                                        uniqueVideos.push(item);
                                    }
                                }
                                
                                return JSON.stringify(uniqueVideos);
                            })();
                        """) { result ->
                                try {
                                    // JSON Array olarak sonucu al
                                    val cleanResult = result.trim('"').replace("\\", "")
                                    val videoArray = JSONArray(cleanResult)

                                    val urls = mutableListOf<String>()
                                    for (i in 0 until videoArray.length()) {
                                        val obj = videoArray.getJSONObject(i)
                                        val url = obj.getString("url")
                                        val quality = obj.getString("quality")

                                        // Sonucu "Kalite | URL" formatında ekle
                                        urls.add("$quality | $url")
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

                loadDataWithBaseURL("https://adultdeepfakes.com/", html, "text/html", "UTF-8", null)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val videoUrls = extractVideoUrls(context, document)

        videoUrls.forEach { url ->
            val split = url.split("|")
            val url = split[1].trim()
            val quality = split[0].trim()
            callback.invoke(
                newExtractorLink(
                    name,
                    name,
                    url,
                    type = ExtractorLinkType.VIDEO,
                    {
                        this.referer = "$mainUrl/"
                        this.quality = getQualityFromName(quality)
                    }
                )
            )
        }

        return videoUrls.isNotEmpty()
    }
}
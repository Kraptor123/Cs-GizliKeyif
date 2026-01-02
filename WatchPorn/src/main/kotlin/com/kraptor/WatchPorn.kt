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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WatchPorn(context: Context) : MainAPI() {
    override var mainUrl              = "https://watchporn.to"
    override var name                 = "WatchPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    private val context = context

    override val mainPage = mainPageOf(
        "${mainUrl}/top-rated/"           to "Top Rated",
        "${mainUrl}/most-popular/"                    to "Most Popular",
        "${mainUrl}/categories/manyvids/"             to "ManyVids",
        "${mainUrl}/categories/onlyfans/"             to "OnlyFans",
        "${mainUrl}/categories/xvideosred/"           to "XVideosRed",
        "${mainUrl}/categories/primalfetish/"         to "PrimalFetish",
        "${mainUrl}/categories/brazzersexxtra/"       to "BrazzersExxtra",
        "${mainUrl}/categories/julesjordan/"          to "JulesJordan",
        "${mainUrl}/categories/pascalssubsluts/"      to "PascalsSubSluts",
        "${mainUrl}/categories/tabooheat/"            to "TabooHeat",
        "${mainUrl}/categories/evilangel/"            to "Evilangel",
        "${mainUrl}/categories/outofthefamily/"       to "OutOfTheFamily",
        "${mainUrl}/categories/missax/"               to "MissaX",
        "${mainUrl}/categories/loveherfeet/"          to "LoveHerFeet",
        "${mainUrl}/categories/mommyblowsbest/"       to "MommyBlowsBest",
        "${mainUrl}/categories/alexlegend/"           to "AlexLegend",
        "${mainUrl}/categories/analized/"             to "Analized",
        "${mainUrl}/categories/analintroductions/"    to "AnalIntroductions",
        "${mainUrl}/categories/blackedraw/"           to "BlackedRaw",
        "${mainUrl}/categories/immeganlive/"          to "ImMeganLive",
        "${mainUrl}/categories/vixen/"                to "Vixen",
        "${mainUrl}/categories/rkprime/"              to "RKPrime",
        "${mainUrl}/categories/puretaboo/"            to "PureTaboo",
        "${mainUrl}/categories/deeper/"               to "Deeper",
        "${mainUrl}/categories/tushy/"                to "Tushy",
        "${mainUrl}/categories/mypervyfamily/"        to "MyPervyFamily",
        "${mainUrl}/categories/familytherapy/"        to "FamilyTherapy",
        "${mainUrl}/categories/hotwifexxx/"           to "HotwifeXXX",
        "${mainUrl}/categories/sislovesme/"           to "SisLovesMe",
        "${mainUrl}/categories/wcaproductions/"       to "WCAProductions",
        "${mainUrl}/categories/jamieyoung/"           to "JamieYoung",
        "${mainUrl}/categories/familystrokes/"        to "FamilyStrokes",
        "${mainUrl}/categories/allherluv/"            to "AllHerLuv",
        "${mainUrl}/categories/blacked/"              to "Blacked",
        "${mainUrl}/categories/tightandteen/"         to "TightAndTeen",
        "${mainUrl}/categories/nubiles/"              to "Nubiles",
        "${mainUrl}/categories/tushyraw/"             to "TushyRaw",
        "${mainUrl}/categories/dadcrush/"             to "DadCrush",
        "${mainUrl}/categories/meana-wolf/"           to "Meana Wolf",
        "${mainUrl}/categories/cosplay/"              to "Cosplay",
        "${mainUrl}/categories/pervmom/"              to "PervMom",
        "${mainUrl}/categories/willtilexxx/"          to "WillTileXXX",
        "${mainUrl}/categories/bangbus/"              to "BangBus",
        "${mainUrl}/categories/mylifeinmiami/"        to "MyLifeInMiami",
        "${mainUrl}/categories/analvids/"             to "AnalVids",
        "${mainUrl}/categories/pornworld/"            to "PornWorld",
        "${mainUrl}/categories/brattysis/"            to "BrattySis",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home     = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("strong.title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-webp"))
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
        val description     = document.selectFirst("div.item:contains(Description:) em")?.text()?.trim()
        val year            = document.selectFirst("div.extra span.C a")?.text()?.trim()?.toIntOrNull()
        val tags            = document.select("div.item:contains(Tags:) a").map { it.text() }
        val score          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()
        val durationText = document.selectFirst("div.item:contains(Duration:) em")
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
        val actors          = document.select("div.item:contains(Models:) a").map { Actor(it.text()) }
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
                                        
                                        // https://watchporn.to/get_file/ ile başlayan .mp4 linklerini bul
                                        var matches = text.match(/https:\/\/watchporn\.to\/get_file\/[^\s'"]+\.mp4[^\s'"]*/g);
                                        
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

                loadDataWithBaseURL("https://watchporn.to/", html, "text/html", "UTF-8", null)
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val videoUrls = extractVideoUrls(context, document)

        videoUrls.forEach { url ->
            Log.d("kraptor_$name", "url = ${url}")
            val quality = url.substringBeforeLast("/").substringAfterLast("/").substringBefore(".").substringAfter("_")
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
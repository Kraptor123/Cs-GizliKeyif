// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Motherless : MainAPI() {
    override var mainUrl              = "https://motherless.com"
    override var name                 = "Motherless"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/porn/public/videos"    to "Public",
        "${mainUrl}/GV02DE763"             to "Best Bodies",
        "${mainUrl}/GV0DC9FD6"             to "Group Sex",
        "${mainUrl}/porn/milf/videos"      to "Milf",
        "${mainUrl}/porn/pov/videos"       to "Pov",
        "${mainUrl}/porn/big-ass/videos"   to "Big Ass",
        "${mainUrl}/porn/big-tits/videos"  to "Big Tits",
        "${mainUrl}/porn/gothic/videos"    to "Gothic",
        "${mainUrl}/porn/lingerie/videos"  to "Lingerie",
        "${mainUrl}/porn/japanese/videos"  to "Japanese",
        "${mainUrl}/porn/german/videos"    to "German",
        "${mainUrl}/porn/vintage/videos"   to "Vintage",
        "${mainUrl}/porn/cfnm/videos"      to "CFNM",
        "${mainUrl}/GV2B87965"             to "Oral",
        "${mainUrl}/GV637AC0A"             to "SHSY",
        "${mainUrl}/GV1AD0514"             to "Best Ama Webcams",
        "${mainUrl}/GV5DBB206"             to "Jerk Off Instructions",
        "${mainUrl}/term/videos/cougar?range=0&size=0&sort=relevance&page="   to "Cougar",
        "${mainUrl}/term/videos/turkish?range=0&size=0&sort=relevance&page="  to "Turkish",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1) {
            app.get("${request.data}").document
        } else if (request.data.contains("page=")) {
            app.get("${request.data}$page").document
        }
        else {
            app.get("${request.data}?page=$page").document
        }
        val home     = document.select("div.thumb-container.video").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title     = this.selectFirst("div.captions a")?.text() ?: return null
        if (title.contains(kirliKelimeRegex)) {
            Log.d("kraptor_$name","igrenc seyler yakalanip silindi")
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/term/${query}").document
        val videolar = fixUrlNull(document.selectFirst("div.content-footer a")?.attr("href")).toString()
        Log.d("kraptor_$name", "videolar = $videolar")
        val videoSayfa = app.get(videolar).document

        return videoSayfa.select("div.thumb-container.video").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title     = this.selectFirst("a.caption.title.pop.plain")?.text() ?: return null
        if (title.contains(kirliKelimeRegex)) {
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title           = document.selectFirst("div.media-meta-title h1")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description     = "Sadece 18 Yaş ve Üzeri İçin Uygundur!"
        val tags            = document.select("div.media-meta-tags a").map { it.text()  }
        if (tags.toString().contains(kirliKelimeRegex)) {
            val title = "Gay veya İğrenç İçerikten Korundun"
            val description = "Gay veya İğrenç İçerikten Korundun Teşekküre Gerek Yok \uD83D\uDE0E"
            val poster      = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ1oeRrakkVr27sgeajl_R8UWsg7ix0gbgOzg&s"
            val urlBos = ""
            return newMovieLoadResponse(title, urlBos, TvType.NSFW, urlBos) {
                this.posterUrl       = poster
                this.plot            = description
                this.tags            = tags
            }
        }
        val recommendations = document.select("div.thumb-container.video").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val kirliKelimeRegex = Regex(desen, RegexOption.IGNORE_CASE)
        val title     = this.selectFirst("div.captions a")?.text() ?: return null
        if (title.contains(kirliKelimeRegex)) {
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img.static")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).text

        val regex = Regex(pattern = "__fileurl = '([^']*)';", options = setOf(RegexOption.IGNORE_CASE))

        val videoUrl = regex.find(document)?.groupValues[1].toString()

        Log.d("kraptor_$name", "videoUrl = ${videoUrl}")

        callback.invoke(newExtractorLink(
            source = "Motherless",
            name   = "Motherless",
            url    = videoUrl,
            type   = INFER_TYPE
        ))
        return true
    }
}
    
    private val igrencKelimeler = listOf(
            "gay", "homosexual", "queer", "homo", "androphile", "femboy", "feminine boy", "effeminate", "trap",
            // Scat & dışkı
            "scat", "coprophilia", "coprophagia", "fecal", "poo", "shit", "crap", "bm play",
            // Kusma ve eşlik eden ifadeler
            "vomit", "puke", "throw up", "barf", "hurl", "spew", "emesis", "regurgitate", "chunder",
            // Bedensel sıvılar
            "urine", "urinate", "pee", "piss", "snot", "mucus",
            // Gore / aşırı şiddet
            "gore", "blood", "splatter", "disembowel", "decapitate", "mutilate", "necrophilia", "bestiality", "zoophilia",
            // Diğer iğrenç fiiller
            "fart", "burp",
            // Genel iğrençlik
            "maggot", "rotten", "decay", "mildew", "mold", "fungus", "toilet bowl", "disgusting",
            // Gaylar
            "Trade", "Vers", "Twink", "Otter", "Bear", "Femme", "Masc", "No fats, no fems", "Serving", "Gagged",
            "G.O.A.T.", "Tea", "Receipts", "Kiki", "Kai Kai", "Werk", "Realness", "Hunty", "Snatched", "Beat",
            "Clocked", "Shade", "Read", "Yas", "Daddy", "Zaddy", "Chosen family", "Closet case", "Out and proud",
            "Henny", "gay", "Queening out", "Slay", "Camp", "Fishy", "Cruising", "Bathhouse", "Power bottom",
            "Situationship", "Pegging", "Anal Gape", "Sick", "Gross", "Femdom", "futa", "futanari", "strap-on", "strapon", "tranny", "tribute"
        )
    
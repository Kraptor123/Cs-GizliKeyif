// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class XXXParodyHD : MainAPI() {
    override var mainUrl              = "https://xxxparodyhd.net"
    override var name                 = "XXXParodyHD"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/movies/"            to "Movies",
//        "${mainUrl}/most-viewed/"                   to "Most Viewed",
//        "${mainUrl}/most-rating/"                   to "En Cok Oylanan",
        "${mainUrl}/genre/free-18-teens/"           to "18+ Teens",
//        "${mainUrl}/genre/free-adventure/"          to "Adventure",
        "${mainUrl}/genre/free-all-girl/"           to "All Girl",
        "${mainUrl}/genre/free-all-sex/"            to "All Sex",
        "${mainUrl}/genre/free-amateurs/"           to "Amateurs",
        "${mainUrl}/genre/free-anal/"               to "Anal",
        "${mainUrl}/genre/free-anal-creampie/"      to "Anal Creampie",
        "${mainUrl}/genre/free-animation/"          to "Animation",
        "${mainUrl}/genre/free-asian/"              to "Asian",
        "${mainUrl}/genre/free-ass-to-mouth/"       to "Ass to Mouth",
        "${mainUrl}/genre/free-babysitter/"         to "Babysitter",
//        "${mainUrl}/genre/free-bbc/"                to "BBC",
//        "${mainUrl}/genre/free-bbw/"                to "BBW",
        "${mainUrl}/genre/free-bdsm/"               to "BDSM",
        "${mainUrl}/genre/free-beach/"              to "Beach",
        "${mainUrl}/genre/free-big-boobs/"          to "Big Boobs",
        "${mainUrl}/genre/free-big-butt/"           to "Big Butt",
        "${mainUrl}/genre/free-big-cocks/"          to "Big Cocks",
//        "${mainUrl}/genre/free-bisexual/"           to "Bisexual",
//        "${mainUrl}/genre/free-black/"              to "Black",
        "${mainUrl}/genre/free-blondes/"            to "Blondes",
        "${mainUrl}/genre/free-blowjobs/"           to "Blowjobs",
        "${mainUrl}/genre/free-brazilian/"          to "Brazilian",
        "${mainUrl}/genre/free-cheerleaders/"       to "Cheerleaders",
        "${mainUrl}/genre/free-college/"            to "College",
        "${mainUrl}/genre/free-cougars/"            to "Cougars",
        "${mainUrl}/genre/free-couples/"            to "Couples",
        "${mainUrl}/genre/free-creampie/"           to "Creampie",
//        "${mainUrl}/genre/free-cuckolds/"           to "Cuckolds",
        "${mainUrl}/genre/free-cumshots/"           to "Cumshots",
        "${mainUrl}/genre/free-czech/"              to "Czech",
        "${mainUrl}/genre/free-deep-throat/"        to "Deep Throat",
//        "${mainUrl}/genre/free-double-anal/"        to "Double Anal",
//        "${mainUrl}/genre/free-double-penetration/" to "Double Penetration",
        "${mainUrl}/genre/free-erotica/"            to "Erotica",
        "${mainUrl}/genre/free-european/"           to "European",
        "${mainUrl}/genre/free-facesitting/"        to "Facesitting",
        "${mainUrl}/genre/free-facials/"            to "Facials",
        "${mainUrl}/genre/free-family-roleplay/"    to "Family Roleplay",
        "${mainUrl}/genre/free-fantasy/"            to "Fantasy",
        "${mainUrl}/genre/free-feature/"            to "Feature",
        "${mainUrl}/genre/free-fetish/"             to "Fetish",
        "${mainUrl}/genre/free-fingering/"          to "Fingering",
        "${mainUrl}/genre/free-gangbang/"           to "Gangbang",
        "${mainUrl}/genre/free-german/"             to "German",
//        "${mainUrl}/genre/free-gonzo/"              to "Gonzo",
//        "${mainUrl}/genre/free-group-sex/"          to "Group Sex",
        "${mainUrl}/genre/free-hairy/"              to "Hairy",
        "${mainUrl}/genre/free-handjobs/"           to "Handjobs",
        "${mainUrl}/genre/free-hardcore/"           to "Hardcore",
        "${mainUrl}/genre/free-hentai/"             to "Hentai",
//        "${mainUrl}/genre/free-indian/"             to "Indian",
//        "${mainUrl}/genre/free-interracial/"        to "Interracial",
        "${mainUrl}/genre/free-italian/"            to "Italian",
        "${mainUrl}/genre/free-japanese/"           to "Japanese",
        "${mainUrl}/genre/free-latin/"              to "Latin",
//        "${mainUrl}/genre/free-parody/"             to "Parody",
        "${mainUrl}/genre/free-lesbian/"            to "Lesbian",
        "${mainUrl}/genre/free-lingerie/"           to "Lingerie",
        "${mainUrl}/genre/free-massage/"            to "Massage",
        "${mainUrl}/genre/free-masturbation/"       to "Masturbation",
        "${mainUrl}/genre/free-mature/"             to "Mature",
        "${mainUrl}/genre/free-milf/"               to "MILF",
        "${mainUrl}/genre/free-mystery/"            to "Mystery",
        "${mainUrl}/genre/free-oiled/"              to "Oiled",
//        "${mainUrl}/genre/free-oral/"               to "Oral",
//        "${mainUrl}/genre/free-orgy/"               to "Orgy",
        "${mainUrl}/genre/free-outdoors/"           to "Outdoors",
        "${mainUrl}/genre/free-parody/"             to "Parody",
        "${mainUrl}/genre/free-pov/"                to "Pov",
        "${mainUrl}/genre/free-public-sex/"         to "Public Sex",
//        "${mainUrl}/genre/free-sex-toy-play/"       to "Sex Toy Play",
        "${mainUrl}/genre/free-small-tits/"         to "Small Tits",
        "${mainUrl}/genre/free-squirting/"          to "Squirting",
        "${mainUrl}/genre/free-stockings/"          to "Stockings",
//        "${mainUrl}/genre/free-swallowing/"         to "Swallowing",
//        "${mainUrl}/genre/free-swingers/"           to "Swingers",
        "${mainUrl}/genre/free-tattoos/"            to "Tattoos",
        "${mainUrl}/genre/free-threesomes/"         to "Threesomes",
//        "${mainUrl}/genre/free-transsexual/"        to "Transsexual",
        "${mainUrl}/genre/free-virgin/"             to "Virgin",
//        "${mainUrl}/genre/free-wives/"              to "Wives",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = if (page == 1){
            app.get("${request.data}").document
        } else {
            app.get("${request.data}page/$page/").document
        }
        val home     = document.select("div.movies-list div.ml-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = if (page == 1){
            app.get("${mainUrl}/search/${query}").document
        } else {
            app.get("${mainUrl}/search/${query}/page/$page/").document
        }

        val aramaCevap = document.select("div.movies-list div.ml-item").mapNotNull { it.toSearchResult() }

        return newSearchResponseList(aramaCevap, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div.mvic-desc h3")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.thumb img")?.attr("src"))
        val description     = document.selectFirst("div.mvic-desc div.desc p")?.text()?.trim()
        val year = document
            .select("div.mvic-desc div.mvici-left p:has(strong:matchesOwn(^Released Date:))")
            .firstOrNull()
            ?.ownText()   // sadece strong dışındaki metni alır
            ?.substringAfter(", ")
            ?.trim()
            ?.toIntOrNull()
        val tags            = document.selectFirst("div.mvici-left p:has(strong:matchesOwn(^Genres:))")
            ?.select("span a")
            ?.map { it.text() }
        val duration = document.selectFirst("p:has(strong:matchesOwn(^Duration))")?.text()?.let {
            val minutes = Regex("""(\d+)\s*mins""").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            minutes
        }
        val recommendations = document.select("div.movies-list div.ml-item").mapNotNull { it.toRecommendationResult() }
        val actors = document.selectFirst("div.mvici-left p:has(strong:matchesOwn(^Pornstars:))")
            ?.select("span a")?.map { Actor(it.text()) }?.toMutableList() ?: mutableListOf()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.select("div.Rtable1 a#\\#iframe")

        iframe.forEach { iframe ->
            val video = iframe.attr("href")
            Log.d("kraptor_$name", "video = ${video}")
            loadExtractor(video, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}
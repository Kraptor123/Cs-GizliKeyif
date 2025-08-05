// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class PornWatch : MainAPI() {
    override var mainUrl              = "https://pornwatch.ws"
    override var name                 = "PornWatch"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/movies-2/"                  to  "Movies",
        "${mainUrl}/xxxfree/"                               to  "Porn Scenes",
        "${mainUrl}/most-viewed-2/"                         to  "Most Viewed",
        "${mainUrl}/most-rating-2/"                         to  "Most Rating",
        "${mainUrl}/director/elegant-angel/"                to  "Elegant Angel",
        "${mainUrl}/director/digital-sin/"                  to  "Digital Sin",
        "${mainUrl}/director/evil-angel/"                   to  "Evil Angel",
        "${mainUrl}/director/new-sensations/"               to  "New Sensations",
        "${mainUrl}/director/private/"                      to  "Private",
        "${mainUrl}/director/marc-dorcel/"                  to  "Marc Dorcel",
        "${mainUrl}/director/brazzers/"                     to  "Brazzers",
        "${mainUrl}/director/wicked-pictures/"              to  "Wicked Pictures",
        "${mainUrl}/director/zero-tolerance/"               to  "Zero Tolerance",
        "${mainUrl}/director/letsdoeit/"                    to  "#LETSDOEIT",
        "${mainUrl}/director/21-sextury-video/"             to  "21 Sextury Video",
        "${mainUrl}/director/3rd-degree/"                   to  "3RD Degree",
        "${mainUrl}/director/abbywinters/"                  to  "abbywinters",
        "${mainUrl}/director/adam-eve/"                     to  "Adam & Eve",
        "${mainUrl}/director/amk-empire/"                   to  "AMK Empire",
        "${mainUrl}/director/analized/"                     to  "Analized",
        "${mainUrl}/director/babes/"                        to  "Babes",
        "${mainUrl}/director/baby-doll-pictures/"           to  "Baby Doll Pictures",
        "${mainUrl}/director/bang-bros-productions/"        to  "Bang Bros",
        "${mainUrl}/director/bang/"                         to  "BANG!",
        "${mainUrl}/director/be-me-fi/"                     to  "be.me.fi",
        "${mainUrl}/director/blacked/"                      to  "Blacked",
        "${mainUrl}/director/blacked-raw/"                  to  "Blacked Raw",
        "${mainUrl}/director/bluebird-films/"               to  "Bluebird Films",
        "${mainUrl}/director/burning-angel-entertainment/"  to  "Burning Angel",
        "${mainUrl}/director/combat-zone/"                  to  "Combat Zone",
        "${mainUrl}/director/depraved-life/"                to  "Depraved Life",
        "${mainUrl}/director/safado/"                       to  "Safado",
        "${mainUrl}/director/xconfessions-by-erika-lust/"   to  "XConfessions",
        "${mainUrl}/director/kink-clips/"                   to  "Kink Clips",
        "${mainUrl}/director/nsfw-films/"                   to  "NSFW Films",
        "${mainUrl}/director/eye-candy/"                    to  "Eye Candy",
        "${mainUrl}/director/she-seduced-me/"               to  "She Seduced Me",
        "${mainUrl}/director/doghouse-digital/"             to  "Doghouse Digital",
        "${mainUrl}/director/purple-bitch/"                 to  "Purple Bitch",
        "${mainUrl}/director/adult-source-media/"           to  "Adult Source Media",
        "${mainUrl}/director/eagle/"                        to  "EAGLE",
        "${mainUrl}/director/jerky-girls/"                  to  "Jerky Girls",
        "${mainUrl}/director/pink-eiga/"                    to  "Pink Eiga",
        "${mainUrl}/director/west-coast-productions/"       to  "West Coast",
        "${mainUrl}/director/mariskax-productions/"         to  "MariskaX",
        "${mainUrl}/director/direct-japanese-imports/"      to  "DirectJapaneseImports",
        "${mainUrl}/director/jay-domino-juicy-niche/"       to  "JayDominoJuicyNiche",
        "${mainUrl}/director/raw-attack/"                   to  "Raw Attack",
        "${mainUrl}/director/team-skeet/"                   to  "Team Skeet",
        "${mainUrl}/director/porn-pros/"                    to  "Porn Pros",
        "${mainUrl}/director/innocent-high/"                to  "Innocent High",
        "${mainUrl}/director/pornfidelity/"                 to  "Pornfidelity",
        "${mainUrl}/director/pinko-tgirls/"                 to  "Pink’o Tgirls",
        "${mainUrl}/director/guilty-desire/"                to  "Guilty Desire",
        "${mainUrl}/director/only3x/"                       to  "Only3x",
        "${mainUrl}/director/blackx/"                       to  "BlackX",
//        "${mainUrl}/genre/18-teens/"                        to  "18+ Teens",
//        "${mainUrl}/genre/adventure/"                       to  "Adventure",
//        "${mainUrl}/genre/all-girl/"                        to  "All Girl",
//        "${mainUrl}/genre/all-sex/"                         to  "All Sex",
//        "${mainUrl}/genre/amateurs/"                        to  "Amateurs",
//        "${mainUrl}/genre/anal/"                            to  "Anal",
//        "${mainUrl}/genre/anal-creampie/"                   to  "Anal Creampie",
//        "${mainUrl}/genre/animation/"                       to  "Animation",
//        "${mainUrl}/genre/asian/"                           to  "Asian",
//        "${mainUrl}/genre/ass-to-mouth/"                    to  "Ass to Mouth",
//        "${mainUrl}/genre/babysitter/"                      to  "Babysitter",
//        "${mainUrl}/genre/bbc/"                             to  "BBC",
//        "${mainUrl}/genre/bbw/"                             to  "BBW",
//        "${mainUrl}/genre/bdsm/"                            to  "BDSM",
//        "${mainUrl}/genre/beach/"                           to  "Beach",
//        "${mainUrl}/genre/big-boobs/"                       to  "Big Boobs",
//        "${mainUrl}/genre/big-butt/"                        to  "Big Butt",
//        "${mainUrl}/genre/big-cocks/"                       to  "Big Cocks",
//        "${mainUrl}/genre/bisexual/"                        to  "Bisexual",
//        "${mainUrl}/genre/black/"                           to  "Black",
//        "${mainUrl}/genre/blondes/"                         to  "Blondes",
//        "${mainUrl}/genre/blowjobs/"                        to  "Blowjobs",
//        "${mainUrl}/genre/brazilian/"                       to  "Brazilian",
//        "${mainUrl}/genre/cheerleaders/"                    to  "Cheerleaders",
//        "${mainUrl}/genre/college/"                         to  "College",
//        "${mainUrl}/genre/cougars/"                         to  "Cougars",
//        "${mainUrl}/genre/couples/"                         to  "Couples",
//        "${mainUrl}/genre/creampie/"                        to  "Creampie",
//        "${mainUrl}/genre/cuckolds/"                        to  "Cuckolds",
//        "${mainUrl}/genre/cumshots/"                        to  "Cumshots",
//        "${mainUrl}/genre/czech/"                           to  "Czech",
//        "${mainUrl}/genre/deep-throat/"                     to  "Deep Throat",
//        "${mainUrl}/genre/double-anal/"                     to  "Double Anal",
//        "${mainUrl}/genre/double-penetration/"              to  "Double Penetration",
//        "${mainUrl}/genre/erotica/"                         to  "Erotica",
//        "${mainUrl}/genre/european/"                        to  "European",
//        "${mainUrl}/genre/facesitting/"                     to  "Facesitting",
//        "${mainUrl}/genre/facials/"                         to  "Facials",
//        "${mainUrl}/genre/family-roleplay/"                 to  "Family Roleplay",
//        "${mainUrl}/genre/fantasy/"                         to  "Fantasy",
//        "${mainUrl}/genre/feature/"                         to  "Feature",
//        "${mainUrl}/genre/fetish/"                          to  "Fetish",
//        "${mainUrl}/genre/fingering/"                       to  "Fingering",
//        "${mainUrl}/genre/gangbang/"                        to  "Gangbang",
//        "${mainUrl}/genre/german/"                          to  "German",
//        "${mainUrl}/genre/gonzo/"                           to  "Gonzo",
//        "${mainUrl}/genre/group-sex/"                       to  "Group Sex",
//        "${mainUrl}/genre/hairy/"                           to  "Hairy",
//        "${mainUrl}/genre/handjobs/"                        to  "Handjobs",
//        "${mainUrl}/genre/hardcore/"                        to  "Hardcore",
//        "${mainUrl}/genre/hentai/"                          to  "Hentai",
//        "${mainUrl}/genre/indian/"                          to  "Indian",
//        "${mainUrl}/genre/interracial/"                     to  "Interracial",
//        "${mainUrl}/genre/italian/"                         to  "Italian",
//        "${mainUrl}/genre/japanese/"                        to  "Japanese",
//        "${mainUrl}/genre/latin/"                           to  "Latin",
//        "${mainUrl}/genre/lesbian/"                         to  "Lesbian",
//        "${mainUrl}/genre/lingerie/"                        to  "Lingerie",
//        "${mainUrl}/genre/massage/"                         to  "Massage",
//        "${mainUrl}/genre/masturbation/"                    to  "Masturbation",
//        "${mainUrl}/genre/mature/"                          to  "Mature",
//        "${mainUrl}/genre/milf/"                            to  "MILF",
//        "${mainUrl}/genre/mystery/"                         to  "Mystery",
//        "${mainUrl}/genre/oiled/"                           to  "Oiled",
//        "${mainUrl}/genre/oral/"                            to  "Oral",
//        "${mainUrl}/genre/orgy/"                            to  "Orgy",
//        "${mainUrl}/genre/lingerie/"                        to  "Lingerie",
//        "${mainUrl}/genre/massage/"                         to  "Massage",
//        "${mainUrl}/genre/masturbation/"                    to  "Masturbation",
//        "${mainUrl}/genre/mature/"                          to  "Mature",
//        "${mainUrl}/genre/milf/"                            to  "MILF",
//        "${mainUrl}/genre/oiled/"                           to  "Oiled",
//        "${mainUrl}/genre/oral/"                            to  "Oral",
//        "${mainUrl}/genre/orgy/"                            to  "Orgy",
//        "${mainUrl}/genre/outdoors/"                        to  "Outdoors",
//        "${mainUrl}/genre/parody/"                          to  "Parody",
//        "${mainUrl}/genre/pov/"                             to  "Pov",
//        "${mainUrl}/genre/public-sex/"                      to  "Public Sex",
//        "${mainUrl}/genre/small-tits/"                      to  "Small Tits",
//        "${mainUrl}/genre/squirting/"                       to  "Squirting",
//        "${mainUrl}/genre/stockings/"                       to  "Stockings",
//        "${mainUrl}/genre/swallowing/"                      to  "Swallowing",
//        "${mainUrl}/genre/swingers/"                        to  "Swingers",
//        "${mainUrl}/genre/tattoos/"                         to  "Tattoos",
//        "${mainUrl}/genre/threesomes/"                      to  "Threesomes",
//        "${mainUrl}/genre/virgin/"                          to  "Virgin",
//        "${mainUrl}/genre/wives/"                           to  "Wives",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.ml-item").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val nohomo = Regex(desen, RegexOption.IGNORE_CASE)
        val title     = this.selectFirst("h2")?.text() ?: return null
        if (title.contains(nohomo)) {
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-lazy-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.ml-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val desen = "\\b(?:${igrencKelimeler.joinToString("|") { Regex.escape(it) }})\\w*\\b"
        val nohomo = Regex(desen, RegexOption.IGNORE_CASE)
        val title     = this.selectFirst("h2")?.text() ?: return null
        if (title.contains(nohomo)) {
            return null
        }
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-lazy-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title           = document.selectFirst("div.mvic-desc h3")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div.thumb img")?.attr("src"))
        val description     = document.selectFirst("div.desc p")?.text()?.trim()
        val year            = document.selectFirst("div.mvici-left > p:nth-child(2)")
            ?.text()
            ?.substringAfterLast(", ")
            ?.trim()
            ?.toIntOrNull()
        val tags            = document.select("div.mvici-left > p:nth-child(4) a").map { it.text().replace("Genres:","") }
        val rating          = document.selectFirst("span.dt_rating_vgs")?.text()?.trim()?.toRatingInt()
        val duration        = document.selectFirst("span.runtime")?.text()?.split(" ")?.first()?.trim()?.toIntOrNull()
        val recommendations = document.select("div.ml-item").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("div.mvici-left > p:nth-child(6) a").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
            this.score           = Score.from10(rating)
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-lazy-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_$name", "data = ${data}")
        val document = app.get(data).document
        val pettabs  = document.selectFirst("div#pettabs")
        pettabs?.select("div.Rtable1-cell a")?.forEach { iframe ->
            val videolar = iframe.attr("href")
            loadExtractor(videolar, "${mainUrl}/", subtitleCallback, callback)
        }
        return true
    }
}

private val igrencKelimeler = listOf(
    "gay", "homosexual", "queer", "homo", "androphile", "femboy", "feminine boy", "effeminate", "trap",
    "Trade", "Vers", "Twink", "Otter", "Bear", "Femme", "Masc", "No fats, no fems", "Serving", "Gagged",
    "Receipts", "Kiki", "Kai Kai", "Werk", "Realness", "Hunty", "Snatched", "Beat",
    "Zaddy", "Chosen family", "Closet case", "Out and proud",
    "Henny", "gay", "Queening out", "Slay", "Camp", "Fishy", "Cruising", "Bathhouse", "Power bottom",
    "Situationship", "Pegging", "Anal Gape", "Sick", "Gross", "Femdom", "futa", "strap-on", "strapon", "tranny", "tribute", "crossdress",
    "t-girl", "tgirl", "Bisexual", "Intersex", "LGBTQ", "Trans"
)

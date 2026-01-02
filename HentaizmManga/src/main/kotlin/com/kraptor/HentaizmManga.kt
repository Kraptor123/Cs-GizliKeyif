// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import androidx.appcompat.app.AppCompatActivity
import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.mapOf


class HentaizmManga(private val plugin: HentaizmMangaPlugin?) : MainAPI() {
    override var mainUrl = "https://manga.hentaizm6.online"
    override var name = "Hentaizm-Manga"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/turler/adult/" to "Adult",
        "${mainUrl}/turler/ahegao/" to "Ahegao",
        "${mainUrl}/turler/aksiyon/" to "Aksiyon",
        "${mainUrl}/turler/aldatma/" to "Aldatma",
        "${mainUrl}/turler/anal/" to "Anal",
        "${mainUrl}/turler/ara-ara/" to "Ara Ara",
        "${mainUrl}/turler/armpit/" to "Armpit",
        "${mainUrl}/turler/asagilama/" to "Aşağılama",
        "${mainUrl}/turler/ayak-fetisi/" to "Ayak Fetişi",
        "${mainUrl}/turler/bakire/" to "Bakire",
        "${mainUrl}/turler/bdsm/" to "BDSM",
        "${mainUrl}/turler/bilim-kurgu/" to "Bilim Kurgu",
        "${mainUrl}/turler/blowjob/" to "Blowjob",
        "${mainUrl}/turler/buyuk-memeler/" to "Büyük Memeler",
        "${mainUrl}/turler/canavar/" to "Canavar",
        "${mainUrl}/turler/cizgi-roman/" to "Çizgi Roman",
        "${mainUrl}/turler/cosplay/" to "Cosplay",
        "${mainUrl}/turler/creampie/" to "Creampie",
        "${mainUrl}/turler/dark-skin/" to "Dark Skin",
        "${mainUrl}/turler/deepthroat/" to "Deepthroat",
        "${mainUrl}/turler/doujinshi/" to "Doujinshi",
        "${mainUrl}/turler/dram/" to "Dram",
        "${mainUrl}/turler/elf/" to "Elf",
        "${mainUrl}/turler/ensest/" to "Ensest",
        "${mainUrl}/turler/fantastik/" to "Fantastik",
        "${mainUrl}/turler/futari/" to "Futari",
        "${mainUrl}/turler/gang-bang/" to "Gang Bang",
        "${mainUrl}/turler/gender-bender/" to "Gender Bender",
        "${mainUrl}/turler/ghost-girl/" to "Ghost Girl",
        "${mainUrl}/turler/grup/" to "Grup",
        "${mainUrl}/turler/hamile/" to "Hamile",
        "${mainUrl}/turler/harem/" to "Harem",
        "${mainUrl}/turler/hayvan/" to "Hayvan",
        "${mainUrl}/turler/hemsire/" to "Hemşire",
        "${mainUrl}/turler/hizmetci/" to "Hizmetçi",
        "${mainUrl}/turler/isekai/" to "Isekai",
        "${mainUrl}/turler/josei/" to "Josei",
        "${mainUrl}/turler/kardes/" to "Kardeş",
        "${mainUrl}/turler/kiz-kardes/" to "Kız Kardeş",
        "${mainUrl}/turler/kole/" to "Köle",
        "${mainUrl}/turler/komedi/" to "Komedi",
        "${mainUrl}/turler/korku/" to "Korku",
        "${mainUrl}/turler/loli/" to "Loli",
        "${mainUrl}/turler/macera/" to "Macera",
        "${mainUrl}/turler/milf/" to "Milf",
        "${mainUrl}/turler/neko/" to "Neko",
        "${mainUrl}/turler/ogretmen/" to "Öğretmen",
        "${mainUrl}/turler/okul/" to "Okul",
        "${mainUrl}/turler/oral/" to "Oral",
        "${mainUrl}/turler/paizuri/" to "Paizuri",
        "${mainUrl}/turler/parodi/" to "Parodi",
        "${mainUrl}/turler/psikolojik/" to "Psikolojik",
        "${mainUrl}/turler/renkli/" to "Renkli",
        "${mainUrl}/turler/romantizm/" to "Romantizm",
        "${mainUrl}/turler/sansursuz/" to "Sansürsüz",
        "${mainUrl}/turler/santaj/" to "Şantaj",
        "${mainUrl}/turler/shota/" to "Shota",
        "${mainUrl}/turler/smell/" to "Smell",
        "${mainUrl}/turler/stuck-in-wall/" to "Stuck in Wall",
        "${mainUrl}/turler/suc/" to "Suç",
        "${mainUrl}/turler/swimsuit/" to "SwimSuit",
        "${mainUrl}/turler/tarihi/" to "Tarihi",
        "${mainUrl}/turler/tecavuz/" to "Tecavüz",
        "${mainUrl}/turler/trap/" to "Trap",
        "${mainUrl}/turler/tren/" to "Tren",
        "${mainUrl}/turler/urophagia/" to "Urophagia",
        "${mainUrl}/turler/vampir/" to "Vampir",
        "${mainUrl}/turler/webtoon/" to "Webtoon",
        "${mainUrl}/turler/yandere/" to "Yandere",
        "${mainUrl}/turler/yaoi/" to "Yaoi",
        "${mainUrl}/turler/yuri/" to "Yuri",
    )


    object SessionManager {
        private var cachedCookies: Map<String, String>? = null
        private val loginMutex = Mutex()

        suspend fun login(): Map<String, String> {
            cachedCookies?.let {
                return it
            }
            return loginMutex.withLock {
                cachedCookies?.let {
                    return@withLock it
                }

                val fresh = app.post(
                    "https://manga.hentaizm6.online/girisyap",
                    data = mapOf(
                        "log" to "bcjqznzkovwuptvhvm",
                        "pwd" to "bcjqznzkovwuptvhvm",
                        "rememberme" to "forever",
                        "wp-submit" to "Oturum+aç",
                        "redirect_to" to "https://manga.hentaizm6.online/wp-admin/",
                        "testcookie" to "1"
                    )
                ).cookies

                cachedCookies = fresh
                fresh
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val cookies = SessionManager.login()
        val document = app.get(
            request.data,
            referer = "${mainUrl}/",
            cookies = cookies
        ).document
        val home = document.select("div.page-listing-item div.col-12").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cookies = SessionManager.login()
        val document = app.get("${mainUrl}/?s=${query}", cookies = cookies).document

        return document.select("div.c-tabs-item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val cookies = SessionManager.login()

        val document = app.get(
            url,
            cookies = cookies
        ).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.summary_image img")?.attr("data-src"))
        val description = "Manga"
        val tags = document.select("div.genres-content a").map { it.text() }
        val mangaId = document.selectFirst("div#manga-chapters-holder")?.attr("data-id") ?: return null

        Log.d("kraptor_Hentai", "mangaId $mangaId")

        val mangaAL = app.post(
            "$mainUrl/wp-admin/admin-ajax.php", cookies = cookies, data = mapOf(
                "action" to "manga_get_chapters",
                "manga" to mangaId
            )
        ).document

        val episodes = mangaAL.select("li.wp-manga-chapter").mapNotNull { manga ->
            val url = manga.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val name = manga.selectFirst("a")?.text() ?: return@mapNotNull null

            newEpisode(url) {
                this.name = name.substringAfter(".", name)
                this.episode = name.substringBefore(".").toIntOrNull()
            }
        }

        return newAnimeLoadResponse(title, url, TvType.NSFW, true) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.episodes = mutableMapOf(
                DubStatus.Subbed to episodes
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("kraptor_$name", "🚀 loadLinks başladı")

        val cookies = SessionManager.login()
        val document = app.get(data, cookies = cookies).document


        val urls = document
            .select("div.page-break.no-gaps img")
            .mapNotNull { it.attr("data-src").trim() }
            .filter { it.isNotEmpty() }

        val number = document
            .select("div.page-break.no-gaps img")
            .mapNotNull { it.attr("id").substringAfter("-").trim() }
            .filter { it.isNotEmpty() }


        val manga = Manga(
            mangaResim = urls,
            mangaBolum = number
        )

        Log.d("kraptor_Hentaizm","Manga Data Class'i ${manga.mangaResim} id = ${manga.mangaBolum}")

        plugin!!.loadChapter(manga)

        return true
    }
}
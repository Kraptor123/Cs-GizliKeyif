// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class Doeda : MainAPI() {
    override var mainUrl              = "https://www.doeda.one"
    override var name                 = "Doeda"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/category/1080p-porno-one/"       to    "1080p",
        "${mainUrl}/category/18-yas-pornosu/"         to    "18+ Yaş",
        "${mainUrl}/category/3d-porno-sex/"           to    "3D",
//        "${mainUrl}/category/480p-pornos/"            to    "480p",
//        "${mainUrl}/category/69-porno-izle/"          to    "69",
//        "${mainUrl}/category/720p-pornos/"            to    "720p",
//        "${mainUrl}/category/7dak/"                   to    "7dak",
        "${mainUrl}/category/turbanli-sex/"           to    "Türbanlı",
        "${mainUrl}/category/turk-yerli-porno/"       to    "Türk",
//        "${mainUrl}/category/turk-ifsa-gizli/"        to    "Türk İfşa",
        "${mainUrl}/category/turkce-porno/"           to    "Türkçe",
        "${mainUrl}/category/turkce-altyazili-tr/"    to    "Türkçe Altyazılı",
        "${mainUrl}/category/turkce-dublaj-p/"        to    "Türkçe Dublaj",
        "${mainUrl}/category/twitter-p/"              to    "Twitter",
        "${mainUrl}/category/abla-pornos/"            to    "Abla",
        "${mainUrl}/category/aile-pornos/"            to    "Aile",
        "${mainUrl}/category/tr-altyazili/"           to    "Altyazılı",
        "${mainUrl}/category/amator/"                 to    "Amatör",
        "${mainUrl}/category/amca-pornos/"            to    "Amca",
        "${mainUrl}/category/amcik-pussy/"            to    "Amcık",
        "${mainUrl}/category/anal/"                   to    "Anal",
        "${mainUrl}/category/animasyon-porno-sex/"    to    "Animasyon",
//        "${mainUrl}/category/anime-porn-sex/"         to    "Anime",
        "${mainUrl}/category/anne-olgun/"             to    "Anne",
        "${mainUrl}/category/arap/"                   to    "Arap",
        "${mainUrl}/category/asyali-cekik-gozlu/"     to    "Asyalı",
//        "${mainUrl}/category/badtv/"                  to    "Badtv",
        "${mainUrl}/category/bakire/"                 to    "Bakire",
        "${mainUrl}/category/baldiz/"                 to    "Baldız",
        "${mainUrl}/category/balik-etli-dolgun/"      to    "Balık Etli",
        "${mainUrl}/category/banyo-dus-klozet/"       to    "Banyo Duş",
//        "${mainUrl}/category/bedava/"                 to    "Bedava",
        "${mainUrl}/category/beyaz-tenli-seks/"       to    "Beyaz Tenli",
        "${mainUrl}/category/blacked-siyah/"          to    "Blacked",
        "${mainUrl}/category/blowjob-ustalari/"       to    "Blowjob",
        "${mainUrl}/category/brazzers-premium/"       to    "Brazzers",
        "${mainUrl}/category/buyuk-meme-gogus/"       to    "Büyük Meme",
        "${mainUrl}/category/canli-yayin-live/"       to    "Canlı Yayın",
//        "${mainUrl}/category/cin-p/"                  to    "Çin",
        "${mainUrl}/category/cinli-p/"                to    "Çinli",
        "${mainUrl}/category/cuce-p/"                 to    "Cüce",
        "${mainUrl}/category/dede-p/"                 to    "Dede",
        "${mainUrl}/category/deep-throat/"            to    "Deep Throat",
        "${mainUrl}/category/doktor-p/"               to    "Doktor",
        "${mainUrl}/category/dovmeli-p/"              to    "Dövmeli",
        "${mainUrl}/category/dul-kadin/"              to    "Dul",
        "${mainUrl}/category/eniste/"                 to    "Enişte",
        "${mainUrl}/category/ensest-p/"               to    "Ensest",
        "${mainUrl}/category/erotik/"                 to    "Erotik",
        "${mainUrl}/category/esmer-guzel/"            to    "Esmer",
        "${mainUrl}/category/evli-surtuk/"            to    "Evli",
        "${mainUrl}/category/evli-cift-p/"            to    "Evli Çift",
//        "${mainUrl}/category/evooli/"                 to    "Evooli",
        "${mainUrl}/category/fantastik-p/"            to    "Fantastik",
        "${mainUrl}/category/fantezi-p/"              to    "Fantezi",
        "${mainUrl}/category/femboy/"                 to    "Femboy",
        "${mainUrl}/category/fetis-p/"                to    "Fetiş",
        "${mainUrl}/category/filmler/"                to    "Filmler",
        "${mainUrl}/category/free-porn/"              to    "Free",
        "${mainUrl}/category/porno-full-hd/"          to    "Full HD",
        "${mainUrl}/category/gangbang-p/"             to    "Gangbang",
        "${mainUrl}/category/gavat-ala/"              to    "Gavat",
//        "${mainUrl}/category/gay-oglan/"              to    "Gay",
        "${mainUrl}/category/genc/"                   to    "Genç",
        "${mainUrl}/category/genel/"                  to    "Genel",
        "${mainUrl}/category/gizli-cekim-porno/"      to    "Gizli Çekim",
        "${mainUrl}/category/gotten/"                 to    "Götten",
        "${mainUrl}/category/grup/"                   to    "Grup",
        "${mainUrl}/category/guney-kore-p/"           to    "Güney Kore",
        "${mainUrl}/category/halloween/"              to    "Halloween",
        "${mainUrl}/category/hamile-kadin/"           to    "Hamile",
        "${mainUrl}/category/hastane-acil/"           to    "Hastane",
        "${mainUrl}/category/hdabla/"                 to    "Hdabla",
        "${mainUrl}/category/hemsire-p/"              to    "Hemşire",
        "${mainUrl}/category/hentai-p/"               to    "Hentai",
        "${mainUrl}/category/horror-porn-korku/"      to    "Horrorporn",
        "${mainUrl}/category/ifsa-gizli/"             to    "İfşa",
        "${mainUrl}/category/ilginc-p/"               to    "İlginç",
//        "${mainUrl}/category/indir/"                  to    "İndir",
        "${mainUrl}/category/iskence-p/"              to    "İşkence",
        "${mainUrl}/category/japon/"                  to    "Japon",
        "${mainUrl}/category/kapali-sex/"             to    "Kapalı",
        "${mainUrl}/category/kardes-p/"               to    "Kardeş",
        "${mainUrl}/category/kari-koca-ev/"           to    "Karı Koca",
        "${mainUrl}/category/kisa-porno/"             to    "Kısa",
        "${mainUrl}/category/konulu/"                 to    "Konulu",
        "${mainUrl}/category/koreli-p/"               to    "Koreli",
        "${mainUrl}/category/korku-p/"                to    "Korku",
        "${mainUrl}/category/kumral-p/"               to    "Kumral",
//        "${mainUrl}/category/kurt/"                   to    "Kürt",
        "${mainUrl}/category/kuzen-p/"                to    "Kuzen",
        "${mainUrl}/category/kuzey-kore-p/"           to    "Kuzey Kore",
        "${mainUrl}/category/ladyboy-shemale/"        to    "Ladyboy",
        "${mainUrl}/category/latin-p/"                to    "Latin",
        "${mainUrl}/category/lez-lezbiyen/"           to    "lez-lezbiyen",
        "${mainUrl}/category/lezbiyen-leziz/"         to    "Lezbiyen",
        "${mainUrl}/category/liseli/"                 to    "Liseli",
        "${mainUrl}/category/maheir/"                 to    "Maheir",
        "${mainUrl}/category/masaj-masoz/"            to    "Masaj",
        "${mainUrl}/category/mature-porn/"            to    "Mature",
        "${mainUrl}/category/milf/"                   to    "Milf",
        "${mainUrl}/category/minyon-p/"               to    "Minyon",
//        "${mainUrl}/category/mobil/"                  to    "Mobil",
        "${mainUrl}/category/odtufans-porno/"         to    "Odtufans",
        "${mainUrl}/category/ogrenci-talebe/"         to    "Öğrenci",
        "${mainUrl}/category/ogretmen-p/"             to    "Öğretmen",
        "${mainUrl}/category/okullu-p/"               to    "Okullu",
        "${mainUrl}/category/olgun/"                  to    "Olgun",
        "${mainUrl}/category/onlyfans-approved/"      to    "OnlyFans",
        "${mainUrl}/category/oral-porno/"             to    "Oral",
        "${mainUrl}/category/orgazm-p/"               to    "Orgazm",
        "${mainUrl}/category/playboy-p/"              to    "Playboy",
        "${mainUrl}/category/porn-xxx/"               to    "Porn",
        "${mainUrl}/category/pornhub-premium/"        to    "Pornhub",
//        "${mainUrl}/category/porno-izle/"             to    "Porno İzle",
//        "${mainUrl}/category/porno-com/"              to    "Porno.com",
        "${mainUrl}/category/pregnant/"               to    "Pregnant",
        "${mainUrl}/category/public-agent-euro/"      to    "Public Agent",
        "${mainUrl}/category/redtube-porn/"           to    "Redtube",
        "${mainUrl}/category/reklamsiz-porno-sitesi/" to    "Reklamsız",
//        "${mainUrl}/category/rokettube-king/"         to    "Rokettube",
        "${mainUrl}/category/rus/"                    to    "Rus",
        "${mainUrl}/category/sakso-p/"                to    "Sakso",
        "${mainUrl}/category/sarisin-p/"              to    "Sarışın",
        "${mainUrl}/category/sert/"                   to    "Sert",
//        "${mainUrl}/category/sex-hikaye-mi-seks/"     to    "Sex Hikaye",
//        "${mainUrl}/category/shemale-tro/"            to    "Shemale",
        "${mainUrl}/category/sisman/"                 to    "Şişman",
//        "${mainUrl}/category/sitesi-porno-sex/"       to    "Sitesi",
//        "${mainUrl}/category/siyah-peynir-izle/"      to    "Siyah Peynir",
        "${mainUrl}/category/squirt-squirting/"       to    "Squirt",
        "${mainUrl}/category/swinger-es-degistirme/"  to    "Swinger",
        "${mainUrl}/category/tayland-pattaya/"        to    "Tayland",
        "${mainUrl}/category/tecavuz/"                to    "Tecavüz",
        "${mainUrl}/category/teyze-ve-kizlari/"       to    "Teyze",
        "${mainUrl}/category/tiktok/"                 to    "TikTok",
        "${mainUrl}/category/tombul-p/"               to    "Tombul",
        "${mainUrl}/category/toplu-p/"                to    "Toplu",
//        "${mainUrl}/category/torun-p/"                to    "Torun",
//        "${mainUrl}/category/travesti-p/"             to    "Travesti",
        "${mainUrl}/category/universiteli-p/"         to    "Üniversiteli",
        "${mainUrl}/category/uvey-abla-p/"            to    "Üvey Abla",
        "${mainUrl}/category/uvey-amca-p/"            to    "Üvey Amca",
        "${mainUrl}/category/uvey-anne-azgin/"        to    "Üvey Anne",
        "${mainUrl}/category/uvey-baba-p/"            to    "Üvey Baba",
        "${mainUrl}/category/uvey-kardes-p/"          to    "Üvey Kardeş",
        "${mainUrl}/category/uvey-kiz-p/"             to    "Üvey Kız",
        "${mainUrl}/category/uzak-dogu-p/"            to    "Uzak Doğu",
        "${mainUrl}/category/uzun-konulu-p/"          to    "Uzun Konulu",
//        "${mainUrl}/category/video/"                  to    "Video",
//        "${mainUrl}/category/x/"                      to    "X",
//        "${mainUrl}/category/xhamster-xxx/"           to    "Xhamster",
//        "${mainUrl}/category/xnxx-porn/"              to    "Xnxx",
//        "${mainUrl}/category/xvideos-one/"            to    "Xvideos",
        "${mainUrl}/category/yabanci-p/"              to    "Yabancı",
        "${mainUrl}/category/yasli/"                  to    "Yaşlı",
        "${mainUrl}/category/yegen-p/"                to    "Yeğen",
        "${mainUrl}/category/yenge-p/"                to    "Yenge",
//        "${mainUrl}/category/yetiskin-p/"             to    "Yetişkin",
//        "${mainUrl}/category/youjizz-porn/"           to    "Youjizz",
//        "${mainUrl}/category/youporn-premium/"        to    "Youporn",
        "${mainUrl}/category/zenci/"                  to    "Zenci",
        "${mainUrl}/category/zorla-seks/"             to    "Zorla",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/$page/").document
        val home     = document.select("div.thumb").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        for (page in 1..4) {
            val url = "$mainUrl/page/$page/?s=${query}"
            val document = app.get(url).document
            val pageResults = document
                .select("div.thumb")
                .mapNotNull { it.toSearchResult() }
            results += pageResults
        }
        return results
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("a")?.attr("title") ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse? {
        val documentapp = app.get(url)
        val document    = documentapp.document
        val doctext     = documentapp.text
        val title           = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val postRegex       = Regex(pattern = "\\{\"@type\":\"ImageObject\",\"url\":\"([^\"]*)\",", options = setOf(RegexOption.IGNORE_CASE))
        val poster          = fixUrlNull(postRegex.find(doctext)?.groupValues[1])
        val description     = document.selectFirst("div.entry-content.rich-content p")?.text()?.trim()
        val year            = document.selectFirst("span.time")?.text()?.substringAfterLast(" ")?.trim()?.toIntOrNull()
        val tags            = document.select("#extras a").map { it.text() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.plot            = description
            this.year            = year
            this.tags            = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_${this.name}", "data = ${data}")
        val document = app.get(data).document

        val iframe   = document.selectFirst("iframe")?.attr("src").toString()

        Log.d("kraptor_${this.name}", "iframe = ${iframe}")

         loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}
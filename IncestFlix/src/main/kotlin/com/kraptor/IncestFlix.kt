// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class IncestFlix : MainAPI() {
    override var mainUrl              = "https://www.incestflix.com/"
    override var name                 = "IncestFlix"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/tag/AAAN"                   to  "Three Aunts, One Nephew/Niece",
        "${mainUrl}/tag/AAMD"                   to  "Two Aunts, Mom, and Daughter",
        "${mainUrl}/tag/AAMS"                   to  "Two Aunts, Mom, and Son",
        "${mainUrl}/tag/AAN"                    to  "Two Aunts, One Nephew",
        "${mainUrl}/tag/AMD"                    to  "Aunt, Mother, Daughter",
        "${mainUrl}/tag/AMS"                    to  "Aunt, Mother, Son",
        "${mainUrl}/tag/AMSD"                   to  "Aunt, Mother, Son, Daughter",
        "${mainUrl}/tag/AMSN"                   to  "Aunt, Mother, Son, Nephew/Niece",
        "${mainUrl}/tag/AN"                     to  "Aunt-Nephew or Aunt-Niece",
        "${mainUrl}/tag/AND"                    to  "Aunt, Nephew, Daughter",
        "${mainUrl}/tag/ANN"                    to  "Aunt and Two Nieces / Aunt and Two Nephews / Aunt, Nephew, Niece",
        "${mainUrl}/tag/ANS"                    to  "Aunt-Nephew-Son or Aunt-Niece-Son",
        "${mainUrl}/tag/Aunt"                   to  "Aunt",
        "${mainUrl}/tag/BBS"                    to  "Two Brothers, One Sister",
        "${mainUrl}/tag/BBSS"                   to  "Two Brothers, Two Sisters",
        "${mainUrl}/tag/Brother"                to  "Brother",
        "${mainUrl}/tag/Brother-in-Law"         to  "Brother in Law",
        "${mainUrl}/tag/BS"                     to  "Brother, Sister",
        "${mainUrl}/tag/BS-InLaw"               to  "Brother-in-Law, Sister-in-Law",
        "${mainUrl}/tag/BSS"                    to  "One Brother, Two Sisters",
        "${mainUrl}/tag/BSSS"                   to  "One Brother, Three Sisters",
        "${mainUrl}/tag/Cousin"                 to  "Cousin",
        "${mainUrl}/tag/CZ"                     to  "Cousins",
        "${mainUrl}/tag/Daughter"               to  "Daughter",
        "${mainUrl}/tag/Daughter-in-Law"        to  "Daughter in Law",
        "${mainUrl}/tag/Father"                 to  "Father",
        "${mainUrl}/tag/Father-in-Law"          to  "Father in Law",
        "${mainUrl}/tag/FD"                     to  "Father, Daughter",
        "${mainUrl}/tag/FD-InLaw"               to  "Father-in-Law, Daughter-in-Law",
        "${mainUrl}/tag/FDA"                    to  "Father, Daughter, Aunt",
        "${mainUrl}/tag/FDD"                    to  "Father and Two Daughters",
        "${mainUrl}/tag/FDDD"                   to  "Father and Three Daughters",
        "${mainUrl}/tag/FDGF"                   to  "Father, Daughter, Grandfather",
        "${mainUrl}/tag/FDN"                    to  "Father, Daughter, Niece",
        "${mainUrl}/tag/FMD"                    to  "Father, Mother, Daughter",
        "${mainUrl}/tag/FMDD"                   to  "Father, Mother and Two Daughters",
        "${mainUrl}/tag/FMS"                    to  "Father, Mother, Son",
        "${mainUrl}/tag/FMSS"                   to  "Father, Mother and Two Sons",
        "${mainUrl}/tag/FS"                     to  "Father, Son",
        "${mainUrl}/tag/FSA"                    to  "Father, Son, Aunt",
        "${mainUrl}/tag/FSD"                    to  "Father, Son, Daughter",
        "${mainUrl}/tag/FSN"                    to  "Father, Son, Niece",
        "${mainUrl}/tag/FSS"                    to  "Father and Two Sons",
        "${mainUrl}/tag/GFFD"                   to  "Grandfather, Father, Daughter",
        "${mainUrl}/tag/GFGD"                   to  "Grandfather, Granddaughter",
        "${mainUrl}/tag/GFGSGD"                 to  "Grandfather, Grandson, Granddaughter",
        "${mainUrl}/tag/GMGD"                   to  "Grandmother, Granddaughter",
        "${mainUrl}/tag/GMGS"                   to  "Grandmother, Grandson",
        "${mainUrl}/tag/GMGSGD"                 to  "Grandmother, Grandson, Granddaughter",
        "${mainUrl}/tag/GMMD"                   to  "Grandmother, Mother, Daughter",
        "${mainUrl}/tag/GMMS"                   to  "Grandmother, Mother, Son",
        "${mainUrl}/tag/Granddaughter"          to  "Granddaughter",
        "${mainUrl}/tag/Grandfather"            to  "Grandfather",
        "${mainUrl}/tag/Grandmother"            to  "Grandmother",
        "${mainUrl}/tag/Grandpa"                to  "Grandpa",
        "${mainUrl}/tag/Grandson"               to  "Grandson",
        "${mainUrl}/tag/InLaws"                 to  "Includes In-Law Relationships",
        "${mainUrl}/tag/MD"                     to  "Mother, Daughter",
        "${mainUrl}/tag/MD-InLaw"               to  "Mother-in-Law, Daughter-in-Law",
        "${mainUrl}/tag/MDD"                    to  "Mother and Two Daughters",
        "${mainUrl}/tag/MDDD"                   to  "Mother and Three Daughters",
        "${mainUrl}/tag/MDGF"                   to  "Mother, Daughter, Grandfather",
        "${mainUrl}/tag/MDN"                    to  "Mother, Daughter, Nephew/Niece",
        "${mainUrl}/tag/MMD"                    to  "Biological Mother, Stepmother, Daughter or Two Stepmothers, One Daughter",
        "${mainUrl}/tag/MMS"                    to  "Biological Mother, Stepmother, Son or Two Stepmothers, One Son",
        "${mainUrl}/tag/Mother"                 to  "Mother",
        "${mainUrl}/tag/Mother-in-Law"          to  "Mother in Law",
        "${mainUrl}/tag/MS"                     to  "Mother, Son",
        "${mainUrl}/tag/MS-InLaw"               to  "Mother-in-Law, Son-in-Law",
        "${mainUrl}/tag/MSD"                    to  "Mother, Son, Daughter",
        "${mainUrl}/tag/MSDD"                   to  "Mother, Son And Two Daughters",
        "${mainUrl}/tag/MSN"                    to  "Mother, Son, Nephew/Niece",
        "${mainUrl}/tag/MSS"                    to  "Mother and Two Sons",
        "${mainUrl}/tag/MSSD"                   to  "Mother, Two Sons and Daughter",
        "${mainUrl}/tag/MSSS"                   to  "Mother and Three Sons",
        "${mainUrl}/tag/Nephew"                 to  "Nephew",
        "${mainUrl}/tag/Niece"                  to  "Niece",
        "${mainUrl}/tag/SIBCZ"                  to  "Siblings with Cousin",
        "${mainUrl}/tag/Siblings"               to  "Siblings",
        "${mainUrl}/tag/Sister"                 to  "Sister",
        "${mainUrl}/tag/Sister-in-Law"          to  "Sister in Law",
        "${mainUrl}/tag/Son"                    to  "Son",
        "${mainUrl}/tag/Son-in-Law"             to  "Son in Law",
        "${mainUrl}/tag/SS"                     to  "Sister, Sister",
        "${mainUrl}/tag/SS-InLaw"               to  "Sister-in-Laws",
        "${mainUrl}/tag/SSS"                    to  "Three Sisters",
        "${mainUrl}/tag/Triplets"               to  "Triplets",
        "${mainUrl}/tag/Twins"                  to  "Twin Sisters or Twin Brother-Sister",
        "${mainUrl}/tag/UAN"                    to  "Uncle, Aunt, Nephew/Niece",
        "${mainUrl}/tag/UFD"                    to  "Uncle, Father, Daughter",
        "${mainUrl}/tag/UMD"                    to  "Uncle, Mother, Daughter",
        "${mainUrl}/tag/UMS"                    to  "Uncle, Mother, Son",
        "${mainUrl}/tag/UN"                     to  "Uncle, Niece",
        "${mainUrl}/tag/Uncle"                  to "Uncle",
        "${mainUrl}/tag/UND"                    to  "Uncle, Niece, Daughter",
        "${mainUrl}/tag/UNN"                    to  "Uncle and Two Nieces, or Uncle-Nephew-Niece",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home     = document.select("#incflix-bodywrap a").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.text-overlay span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.img-overflow")?.attr("style")?.substringBefore(")")
            ?.substringAfter("url("))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }


    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("h2")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div video")?.attr("poster"))
        val tags            = document.select("a.studiolink3").map { it.text() }
        val recommendations = document.select("a#videolink").mapNotNull { it.toRecommendationResult() }
        val actors          = document.select("a.studiolink1").map { Actor(it.text()) }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val title     = this.selectFirst("div.text-overlay span")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.img-overflow")?.attr("style")?.substringBefore(")")
            ?.substringAfter("url("))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("kraptor_${this.name}", "data = ${data}")
        val document = app.get(data).document

        val iframe   = fixUrlNull(document.selectFirst("source")?.attr("src")).toString()
        Log.d("kraptor_${this.name}", "iframe = ${iframe}")

        callback.invoke(newExtractorLink(
            source = this.name,
            name   = this.name,
            url    = iframe,
            type =   ExtractorLinkType.VIDEO
            ))

        return true
    }
}
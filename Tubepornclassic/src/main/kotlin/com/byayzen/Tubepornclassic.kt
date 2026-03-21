// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import android.util.Base64
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Tubepornclassic : MainAPI() {
    override var mainUrl = "https://tubepornclassic.com"
    override var name = "Tubepornclassic"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "classic" to "Classic",
        "vintage" to "Vintage",
        "retro" to "Retro",
        "categories-brunette" to "Brunette",
        "big-boobs" to "Big Tits",
        "milf" to "MILF",
        "categories-anal" to "Anal",
        "categories-blonde" to "Blond",
        "blonde" to "Blonde",
        "big-cock" to "Big Cock",
        "hairy" to "Hairy",
        "stockings" to "Stockings",
        "threesome" to "Threesome",
        "categories-facial" to "Facial",
        "deep-throat" to "Deep Throat",
        "group" to "Group Sex",
        "big-butts" to "Big Ass",
        "cumshot" to "Cumshot",
        "deepthroat" to "Deepthroat",
        "categories-ir" to "Interracial",
        "teens" to "Teens (18+)",
        "outdoor" to "Outdoor",
        "categories-lesbian" to "Lesbian",
        "cunnilingus" to "Cunnilingus",
        "sex-toys" to "Toys",
        "fetish" to "Fetish",
        "categories-dp" to "Double Penetration",
        "amateur" to "Amateur",
        "mature" to "Mature",
        "categories-redhead" to "Red Head",
        "categories-black" to "Ebony",
        "tattoo" to "Tattoo",
        "old-young" to "Old and Young (18+)",
        "lingerie" to "Lingerie",
        "blowjob" to "Blowjob",
        "gangbang" to "Gangbang",
        "hardcore" to "Hardcore",
        "small-tits" to "Small Tits",
        "info-compilation" to "Compilation",
        "categories-softcore" to "Softcore",
        "categories-asian" to "Asian",
        "bdsm" to "BDSM",
        "cum-in-mouth" to "Cum In Mouth",
        "handjobs" to "Handjob",
        "fingering" to "Fingering",
        "german" to "German",
        "categories-creampie" to "Creampie",
        "bondage" to "Bondage",
        "french" to "French",
        "pov" to "Point of View",
        "bbw" to "BBW",
        "pov2" to "POV",
        "categories-latin" to "Latina",
        "italian" to "Italian",
        "step-fantasy" to "Step Fantasy",
        "categories-masturbation" to "Masturbation",
        "categories-fisting" to "Fisting",
        "public" to "Public",
        "solo-female" to "Solo Female",
        "strapon" to "Strapon"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/api/json/videos2/86400/str/latest-updates/60/categories.${request.data}.$page.all...json"
        val res = app.get(url).parsedSafe<ResponseJson>()
        val home = res?.videos?.mapNotNull { video ->
            val title = video.title ?: return@mapNotNull null
            newMovieSearchResponse(title, "$mainUrl/videos/${video.video_id}/${video.dir}/", TvType.NSFW) {
                val binid = (video.video_id?.toIntOrNull() ?: 0) / 1000 * 1000
                this.posterUrl = video.scr ?: "https://tn.tubepornclassic.com/contents/videos_screenshots/$binid/${video.video_id}/240x180/2.jpg"
            }
        } ?: emptyList()
        return newHomePageResponse(request.name, home, true)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val searchurl = "$mainUrl/api/videos2.php?params=86400/str/relevance/60/search..$page.all..&s=$query"
        val res = app.get(searchurl).parsedSafe<ResponseJson>()
        val results = res?.videos?.mapNotNull { video ->
            val title = video.title ?: return@mapNotNull null
            newMovieSearchResponse(title, "$mainUrl/videos/${video.video_id}/${video.dir}/", TvType.NSFW) {
                val binid = (video.video_id?.toIntOrNull() ?: 0) / 1000 * 1000
                this.posterUrl = video.scr ?: "https://tn.tubepornclassic.com/contents/videos_screenshots/$binid/${video.video_id}/240x180/2.jpg"
            }
        } ?: emptyList()
        return newSearchResponseList(results, (res?.pages ?: 0) > page)
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = Regex("""/videos/(\d+)/""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val bin2 = (id / 1000) * 1000
        val bin1 = (id / 1000000) * 1000000

        val videoapiurl = "$mainUrl/api/json/video/86400/0/$bin2/$id.json"
        val videores = app.get(videoapiurl).parsedSafe<VideoDetailsResponse>()?.video ?: return null

        val recurl = "$mainUrl/api/json/videos_related2/432000/36/$bin1/$bin2/$id.all.1.json"
        val recommendations = app.get(recurl).parsedSafe<ResponseJson>()?.videos?.mapNotNull { video ->
            val rectitle = video.title ?: return@mapNotNull null
            newMovieSearchResponse(rectitle, "$mainUrl/videos/${video.video_id}/${video.dir}/", TvType.NSFW) {
                val recbinid = (video.video_id?.toIntOrNull() ?: 0) / 1000 * 1000
                this.posterUrl = video.scr ?: "https://tn.tubepornclassic.com/contents/videos_screenshots/$recbinid/${video.video_id}/240x180/1.jpg"
            }
        }

        return newMovieLoadResponse(videores.title ?: "", url, TvType.NSFW, url) {
            this.posterUrl = videores.thumb ?: videores.thumbsrc
            this.plot = videores.description
            this.tags = videores.tags?.values?.mapNotNull { it.title }
            videores.models?.values?.mapNotNull { it.title }?.map { Actor(it) }?.let { addActors(it) }
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val videoid = Regex("""/videos/(\d+)/""").find(data)?.groupValues?.get(1) ?: return false
        val apifileurl = "$mainUrl/api/videofile.php?video_id=$videoid&lifetime=8640000"

        val response = app.get(apifileurl).text
        val videourlmatch = Regex(""""video_url":"(.*?)"""").find(response)?.groupValues?.get(1) ?: return false

        val base64clean = videourlmatch
            .replace("\\u0410", "A").replace("\\u0412", "B").replace("\\u0421", "C").replace("\\u0415", "E").replace("\\u041d", "H")
            .replace("\\u041a", "K", true).replace("\\u041c", "M", true).replace("\\u041e", "O", true)
            .replace("\\u0420", "P", true).replace("\\u0422", "T", true).replace("\\u0425", "X", true)
            .replace("\\u0430", "a").replace("\\u0441", "c").replace("\\u0445", "x").replace("\\u0443", "y")
            .replace("А", "A").replace("В", "B").replace("С", "C").replace("Е", "E").replace("М", "M").replace("О", "O").replace("Р", "P")
            .replace(",", "/").replace("~", "=").replace("-", "+").replace("_", "/").trim()

        val getfilepath = try { String(Base64.decode(base64clean, Base64.DEFAULT)) } catch (e: Exception) { "" }
        if (getfilepath.isBlank()) return false

        val fullurl = if (getfilepath.startsWith("http")) getfilepath else "$mainUrl$getfilepath"
        val finalurl = app.get(fullurl, referer = "$mainUrl/", allowRedirects = false).headers["location"] ?: fullurl

        callback(
            newExtractorLink(
                source = name,
                name = name,
                url = finalurl,
                type = if (finalurl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.P720.value
                this.headers = mapOf("User-Agent" to "Mozilla/5.0", "Accept" to "*/*")
            }
        )
        return true
    }

    data class ResponseJson(val videos: List<Video>? = null, val pages: Int? = null)
    data class Video(val video_id: String? = null, val title: String? = null, val dir: String? = null, val scr: String? = null)
    data class VideoDetailsResponse(val video: VideoDetails? = null)
    data class VideoDetails(val title: String? = null, val description: String? = null, val thumb: String? = null, val thumbsrc: String? = null, val tags: Map<String, TagItem>? = null, val models: Map<String, ModelItem>? = null)
    data class TagItem(val title: String? = null)
    data class ModelItem(val title: String? = null)
}
// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.byayzen

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.*

class PMVHaven : MainAPI() {
    override var mainUrl        = "https://pmvhaven.com"
    override var name           = "PMVHaven"
    override val hasMainPage    = true
    override var lang           = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus      = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/api/videos?limit=32&sort=-releaseDate&tagMode=OR&expandTags=false"           to "New",
        "${mainUrl}/api/videos?limit=32&sort=-views&tagMode=OR&expandTags=false&trending=true"  to "Trending",
        "${mainUrl}/api/videos?limit=32&sort=-likes&tagMode=OR&expandTags=false"                to "Most Liked",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=blowjob&tagMode=OR&expandTags=false"   to "Blowjob",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=big ass&tagMode=OR&expandTags=false"   to "Big Ass",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=big boobs&tagMode=OR&expandTags=false" to "Big Boobs",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=cumshot&tagMode=OR&expandTags=false"   to "Cumshot",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=teen&tagMode=OR&expandTags=false"      to "Teen",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=riding&tagMode=OR&expandTags=false"    to "Riding",
        "${mainUrl}/api/videos?limit=32&sort=-views&tags=bbc&tagMode=OR&expandTags=false"       to "BBC"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        Log.d(name, "MaiN page başladı")
        val url  = "${request.data}&page=$page"
        val json = app.get(url).parsedSafe<PMVHavenResponse>()
        val home = json?.videos
            ?.filter { it.hasGayContent != true && it.hasTransContent != true }
            ?.mapNotNull { VideoLinktutucu(it._id, it.title, it.thumbnailUrl) }
            ?: emptyList()

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name               = request.name,
                    list               = home,
                    isHorizontalImages = true
                )
            ),
            hasNext = json?.pagination?.hasNext == true
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        Log.d(name, "Search Başladı")
        val url  = "${mainUrl}/api/videos/search?limit=32&page=$page&q=$query"
        val json = app.get(url).parsedSafe<PMVHavenResponse>()
        val list = json?.videos
            ?.filter { it.hasGayContent != true && it.hasTransContent != true }
            ?.mapNotNull { VideoLinktutucu(it._id, it.title, it.thumbnailUrl) }
            ?: emptyList()

        return newSearchResponseList(list, hasNext = list.size >= 32)
    }

    override suspend fun quickSearch(query: String) = search(query)

    override suspend fun load(url: String): LoadResponse? {
        Log.d(name, "Load Başladı")
        val id   = Regex("/watch/([a-zA-Z0-9]+)").find(url)?.groupValues?.get(1) ?: return null
        val api  = "${mainUrl}/api/videos/$id/watch-page"
        val data = app.get(api).parsedSafe<WatchPageResponse>()?.data ?: return null
        val v    = data.video ?: return null
        if (v.hasGayContent == true || v.hasTransContent == true) return null
        val title    = v.title ?: return null
        val poster   = v.thumbnailUrl
        val plot     = v.description
        val year     = v.uploadDate?.substring(0, 4)?.toIntOrNull()
        val tags     = (v.tags ?: emptyList()) + (v.starsTags ?: emptyList())
        val score    = v.bayesianRating
        val duration = v.durationSeconds?.div(60)
        val actors   = v.starsTags?.map { Actor(it) } ?: emptyList()
        val recs     = data.recommendedVideos?.mapNotNull { VideoLinktutucu(it._id, it.title, it.thumbnailUrl) } ?: emptyList()

        return newMovieLoadResponse(title, url, TvType.NSFW, id) {
            this.posterUrl       = poster
            this.plot            = plot
            this.year            = year
            this.tags            = tags
            this.score           = Score.from(score, 100)
            this.duration        = duration
            this.recommendations = recs
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(name, "Loadlinks Başladı")
        val api     = "${mainUrl}/api/videos/$data/watch-page"
        val v       = app.get(api).parsedSafe<WatchPageResponse>()?.data?.video ?: return false
        val hls     = v.hlsMasterPlaylistUrl
        val mp4     = v.videoUrl
        val quality = v.height ?: Qualities.Unknown.value
        val url     = hls ?: mp4 ?: return false
        val type    = if (hls != null) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO

        callback(
            newExtractorLink(
                source = name,
                name   = name,
                url    = url,
                type   = type
            ) {
                this.quality = quality
            }
        )
        return true
    }

    private fun VideoLinktutucu(id: String?, title: String?, poster: String?): SearchResponse? {
        val t = title ?: return null
        val i = id ?: return null
        return newMovieSearchResponse(t, "${mainUrl}/watch/$i", TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    data class PMVHavenResponse(
        val videos: List<VideoItem>?,
        val pagination: Pagination?
    )

    data class VideoItem(
        val _id: String?,
        val title: String?,
        val thumbnailUrl: String?,
        val hasGayContent: Boolean?,
        val hasTransContent: Boolean?
    )

    data class WatchPageResponse(
        val data: WatchData?
    )

    data class WatchData(
        val video: VideoDetail?,
        val recommendedVideos: List<RecVideo>?
    )

    data class VideoDetail(
        val _id: String?,
        val title: String?,
        val description: String?,
        val thumbnailUrl: String?,
        val uploadDate: String?,
        val durationSeconds: Int?,
        val bayesianRating: Double?,
        val tags: List<String>?,
        val starsTags: List<String>?,
        val hlsMasterPlaylistUrl: String?,
        val videoUrl: String?,
        val height: Int?,
        val hasGayContent: Boolean?,
        val hasTransContent: Boolean?
    )

    data class RecVideo(
        val _id: String?,
        val title: String?,
        val thumbnailUrl: String?
    )

    data class Pagination(
        val hasNext: Boolean?
    )
}
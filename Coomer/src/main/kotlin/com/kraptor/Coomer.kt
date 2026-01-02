// ! Bu araç @kerimmkirac + Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor

import com.lagradost.api.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import java.util.Collections

data class Creator(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("service") val service: String,
    @JsonProperty("indexed") val indexed: Long,
    @JsonProperty("updated") val updated: Long,
    @JsonProperty("favorited") val favorited: Int,
)

class Coomer (val plugin: CoomerPlugin) : MainAPI() {
    override var mainUrl = "https://coomer.st"
    override var name = "Coomer"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/api/v1/creators.txt" to "Creators"
    )

    private val coomerHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
        "Referer" to "https://coomer.st/",
        "Accept" to "text/css"
    )

    private suspend fun fetchCreators(): List<Creator> {
        val jsonText =
            app.get("https://raw.githubusercontent.com/Kraptor123/Cs-GizliKeyif/refs/heads/master/.github/commer.json")
                .textLarge
                .let { Jsoup.parse(it).body().text() }

        return jacksonObjectMapper().readValue(jsonText)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val creators = fetchCreators().shuffled()

        val chunked = creators.chunked(4000)

        val homePageLists = chunked.mapIndexed { index, group ->
            val items = group.map { creator ->
                newMovieSearchResponse(
                    creator.name,
                    "$mainUrl/api/v1/${creator.service}/user/${creator.id}/profile",
                    TvType.NSFW
                ) {
                    posterUrl = "https://img.coomer.st/icons/${creator.service}/${creator.id}"
                }
            }

            HomePageList(
                name = "${request.name} ${index + 1}",  // Örn: creators 1, creators 2
                list = items,
                isHorizontalImages = true
            )
        }

        return newHomePageResponse(homePageLists, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return emptyList()
        return fetchCreators()
            .filter { it.name.contains(query, true) || it.id.contains(query, true) }
            .take(50)
            .map {
                newMovieSearchResponse(it.name, "$mainUrl/api/v1/${it.service}/user/${it.id}/profile", TvType.NSFW) {
                    posterUrl = "https://img.coomer.st/icons/${it.service}/${it.id}"
                }
            }
    }

    override suspend fun load(url: String): LoadResponse? {
        val profileMap: Map<String, Any> = jacksonObjectMapper()
            .readValue(app.get(url, headers = coomerHeaders).text)
        val service = url.substringAfter("/v1/").substringBefore("/")
        val id = url.substringAfter("/user/").substringBefore("/")
        val name = profileMap["name"]?.toString().orEmpty()
        val banner = "https://img.coomer.st/banners/$service/$id"
//        Log.d("kraptor_coomer","url = $url")

        // Async olarak pagination ile post çekme
        val allPosts = Collections.synchronizedList(mutableListOf<Post>())
        val mapper = jacksonObjectMapper()

        withTimeoutOrNull(3000) {
            coroutineScope {
                // İlk sayfayı senkron çek
                val firstPageUrl = "$mainUrl/api/v1/$service/user/$id/posts"
                try {
                    val firstPageGet = app.get(firstPageUrl, headers = coomerHeaders).textLarge
                    val firstPagePosts: List<Post> = mapper.readValue(firstPageGet, object : com.fasterxml.jackson.core.type.TypeReference<List<Post>>() {})
                    allPosts.addAll(firstPagePosts)
//                    Log.d("kraptor_coomer","First page loaded: ${firstPagePosts.size} posts")
                } catch (e: Exception) {
//                    Log.e("kraptor_coomer", "Error loading first page: ${e.message}")
                    return@coroutineScope
                }

                // Batch'ler halinde paralel yükle (her batch'te 5 sayfa)
                var offset = 50
                val batchSize = 5

                while (offset <= 5000) {
                    val batchJobs = mutableListOf<Deferred<Pair<Int, List<Post>?>>>()

                    // Bir batch'teki job'ları başlat
                    repeat(batchSize) {
                        val currentOffset = offset + (it * 50)
                        if (currentOffset > 5000) return@repeat

                        val job = async {
                            try {
                                val pageUrl = "$mainUrl/api/v1/$service/user/$id/posts?o=$currentOffset"
                                val response = app.get(pageUrl, headers = coomerHeaders)
                                val pageGet = response.textLarge

                                // Hata mesajı varsa null dön
                                if (pageGet.contains("\"error\"")) {
//                                    Log.d("kraptor_coomer","Reached end at offset $currentOffset")
                                    return@async Pair(currentOffset, null)
                                }

                                val pagePosts: List<Post> = mapper.readValue(pageGet, object : com.fasterxml.jackson.core.type.TypeReference<List<Post>>() {})

                                if (pagePosts.isEmpty()) {
//                                    Log.d("kraptor_coomer","Empty page at offset $currentOffset")
                                    return@async Pair(currentOffset, null)
                                }

//                                Log.d("kraptor_coomer","Loaded offset $currentOffset: ${pagePosts.size} posts")
                                Pair(currentOffset, pagePosts)

                            } catch (e: Exception) {
//                                Log.d("kraptor_coomer", "Error at offset $currentOffset")
                                Pair(currentOffset, null)
                            }
                        }
                        batchJobs.add(job)
                    }

                    // Batch'teki tüm job'ları bekle
                    val results = batchJobs.awaitAll()

                    // Sonuçları sıralı olarak ekle ve hata kontrolü yap
                    var shouldStop = false
                    results.sortedBy { it.first }.forEach { (_, posts) ->
                        if (posts == null) {
                            shouldStop = true
                        } else if (!shouldStop) {
                            allPosts.addAll(posts)
                        }
                    }

                    // Hata aldıysak dur
                    if (shouldStop) {
//                        Log.d("kraptor_coomer","Stopping pagination, total posts: ${allPosts.size}")
                        break
                    }

                    offset += (batchSize * 50)
                }
            }
        }

//        Log.d("kraptor_coomer","Final total posts loaded: ${allPosts.size}")

        val episodes = mutableListOf<Episode>()
        val allImages = mutableListOf<String>()

        fun isImage(path: String): Boolean {
            val lower = path.lowercase()
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
        }

        fun isVideo(path: String): Boolean {
            val lower = path.lowercase()
            return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")
        }

        for (post in allPosts) {
            post.file.path?.let { p ->
                if (isImage(p)) {
                    allImages.add("https://coomer.st/data$p")
                }
            }
            post.attachments.forEach { att ->
                att.path?.let { p ->
                    if (isImage(p)) {
                        allImages.add("https://coomer.st/data$p")
                    }
                }
            }
        }

        if (allImages.isNotEmpty()) {
            episodes.add(
                newEpisode(
                    url = "IMAGES::" + allImages.joinToString("||"),
                    {
                        this.name = "Fotoğraflar (${allImages.size} adet)"
                        episode = 1
                    }
                )
            )
//            Log.d("kraptor_coomer","Added image episode with ${allImages.size} images")
        }

        // Video bölümlerini ekle
        var episodeNumber = 2
        for (post in allPosts) {
            val videoUrls = mutableListOf<String>()
            var thumbnailUrl: String? = null

            // Ana dosya video mu kontrol et
            post.file.path?.let { p ->
                if (isVideo(p)) {
                    val videoUrl = "https://coomer.st/data$p"
                    videoUrls.add(videoUrl)
//                    Log.d("kraptor_coomer","Video found: $videoUrl")
                } else if (isImage(p)) {
                    thumbnailUrl = "https://img.coomer.st/thumbnail/data$p"
                }
            }

            // Eklerdeki videoları kontrol et
            post.attachments.forEach { att ->
                att.path?.let { p ->
                    if (isVideo(p)) {
                        val videoUrl = "https://coomer.st$p"
                        videoUrls.add(videoUrl)
                    }
                }
            }

            // Video varsa bölüm olarak ekle
            if (videoUrls.isNotEmpty()) {
                val episodeData = "VIDEOS::" + videoUrls.joinToString("||")
                val name = post.title?.take(50)
                val bolum = if (name?.isEmpty() == true){
                    "Video ${episodeNumber - 1}"
                } else {
                    name
                }
                episodes.add(
                    newEpisode(
                        url = episodeData,
                        {
                            this.name = bolum
                            episode = episodeNumber
                            posterUrl = thumbnailUrl
                        }
                    )
                )
                episodeNumber++
            }
        }
        return newTvSeriesLoadResponse(name, url, TvType.NSFW, episodes) {
            posterUrl = banner
            plot = "18 Yaş ve Üzeri İçin Uygundur! Creator: $name\nService: $service\n"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        when {
            data.contains("IMAGES::") -> {
                val imagesPart = data.substringAfter("IMAGES::")
                val images = imagesPart.split("||")
                plugin.loadChapter(images)
            }
            data.contains("VIDEOS::") -> {
                // Video bölümü
                val videosPart = data.substringAfter("VIDEOS::")
                val videos = videosPart.split("||")
                videos.forEachIndexed { index, videoUrl ->
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name Video ${index + 1}",
                            url = videoUrl,
                            type = ExtractorLinkType.VIDEO,
                            {
                                this.referer = "${mainUrl}/"
                                this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.7103.48 Safari/537.36")
                                this.quality = Qualities.Unknown.value
                            }
                        )
                    )
                }
            }
        }

        return true
    }
}


data class Post(
    val id: String,
    val user: String,
    val service: String,
    val title: String?,
    val substring: String?,
    val published: String?,
    val file: FileEntry = FileEntry(),                // default boş
    val attachments: List<FileEntry> = emptyList()     // default boş liste
)

data class FileEntry(
    val name: String? = null,
    val path: String? = null
)
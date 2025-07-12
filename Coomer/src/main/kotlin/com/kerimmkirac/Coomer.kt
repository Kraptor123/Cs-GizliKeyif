// ! Bu araç @kerimmkirac + Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.Jsoup

data class Creator(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("service") val service: String,
    @JsonProperty("indexed") val indexed: Long,
    @JsonProperty("updated") val updated: Long,
    @JsonProperty("favorited") val favorited: Int,
)

class Coomer (val plugin: CoomerPlugin) : MainAPI() {
    override var mainUrl = "https://coomer.su"
    override var name = "Coomer"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/api/v1/creators.txt" to "Creators"
    )

    private suspend fun fetchCreators(): List<Creator> {
        val jsonText = app.get("${mainUrl}/api/v1/creators.txt")
            .textLarge
            .let { Jsoup.parse(it).body().text() }

        return jacksonObjectMapper().readValue(jsonText)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val creators = fetchCreators().take(500)
        val home = creators.map {
            newMovieSearchResponse(it.name, "$mainUrl/api/v1/${it.service}/user/${it.id}/profile", TvType.NSFW) {
                posterUrl = "https://img.coomer.su/icons/${it.service}/${it.id}"
            }
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return emptyList()
        return fetchCreators()
            .filter { it.name.contains(query, true) || it.id.contains(query, true) }
            .take(50)
            .map {
                newMovieSearchResponse(it.name, "$mainUrl/api/v1/${it.service}/user/${it.id}/profile", TvType.NSFW) {
                    posterUrl = "https://img.coomer.su/icons/${it.service}/${it.id}"
                }
            }
    }

    override suspend fun load(url: String): LoadResponse? {
        val profileMap: Map<String, Any> = jacksonObjectMapper()
            .readValue(app.get(url).text)
        val service = url.substringAfter("/v1/").substringBefore("/")
        val id = url.substringAfter("/user/").substringBefore("/")
        val name = profileMap["name"]?.toString().orEmpty()
        val banner = "https://img.coomer.su/banners/$service/$id"

        return newMovieLoadResponse(name, url, TvType.NSFW, url) {
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
        // Kullanıcı bilgisi
        val jsonText = app.get(data).textLarge
        val jsonMap = jacksonObjectMapper().readValue<Map<String, Any>>(jsonText)
        val user = jsonMap["name"].toString()
        val service = jsonMap["service"].toString()

        // Post listesini çek
        val postUrl = "$mainUrl/api/v1/$service/user/$user/posts-legacy?o=100"
        val postGet = app.get(postUrl).textLarge

        // JSON'u Post sınıfına çevir
        val mapper = jacksonObjectMapper()
        val postsJson = mapper.readTree(postGet).get("results").toString()
        val posts: List<Post> = mapper.readValue(postsJson, object : TypeReference<List<Post>>() {})

        // Görselleri topla
        val allImages = mutableListOf<String>()
        for (post in posts) {
            post.file.path?.let { p ->
                Log.d("kraptor_$this","p = ${"https://img.coomer.su/thumbnail/data$p"}")
                allImages.add("https://img.coomer.su/thumbnail/data$p").toString().trim()
            }
            post.attachments.forEach { att ->
                att.path?.let { p ->
                    allImages.add("https://img.coomer.su/thumbnail/data$p")
                }
            }
        }

        // Başlık ve görselleri yükle
        plugin.loadChapter(user, allImages)
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
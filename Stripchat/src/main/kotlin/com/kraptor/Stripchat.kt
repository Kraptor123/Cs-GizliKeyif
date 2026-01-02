package com.kraptor

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.kraptor.decodeM3u8MouflonFilesFixed
import java.io.File

class Stripchat(context: Context) : MainAPI() {
    override var mainUrl = "https://stripchat.com"
    override var name = "Stripchat"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded
    private val apiUrl = "$mainUrl/api/front/models/get-list"
    private val excludeIdsMap: MutableMap<String, MutableList<Int>> = mutableMapOf()

    private val context = context

    override val mainPage = mainPageOf(
        "tagLanguageTurkish" to "Turkish",
        "ageTeen" to "Teen",
        "ageMilf" to "Milf",
        "ageMature" to "Mature",
        "ethnicityAsian" to "Asian",
        "ethnicityLatino" to "Latina",
        "ethnicityEbony" to "Ebony",
        "bodyTypeCurvy" to "Curvy",
        "bodyTypePetite" to "Petite",
        "bodyTypeAthletic" to "Athletic",
        "specificsBigTits" to "Big Tits",
        "specificsBigAss" to "Big Ass",
        "girls" to "Girls",
        "couples" to "Couples",
//            "men" to "Men",
//            "trans" to "Trans",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val limit = 60
// Eğer page 1 tabanlı geliyorsa 1'i çıkar, değilse 0 kalır
        val pageIndex = if (page <= 0) 0 else page - 1
        val offset = pageIndex * limit
        val eList: MutableList<Int> = mutableListOf()

        val responseList =  if (request.data.equals("couples", ignoreCase = true) ||
            request.data.equals("girls", ignoreCase = true)) {
            val map = mapOf(
                "favoriteIds" to mutableListOf<String>(),
                "limit" to limit,
                "offset" to offset,
                "primaryTag" to request.data,
                "sortBy" to "viewersRating",
                "userRole" to "guest",
                "improveTs" to false,
                "excludeModelIds" to (excludeIdsMap[request.data] ?: mutableListOf<Int>()),
                "isRecommendationDisabled" to false,
            )

            val resp = app.post(apiUrl, json = map).parsedSafe<Response>()
            val rawCount = resp?.models?.size ?: 0

            resp?.models?.map { model ->
                eList.add(model.id.toInt())
                newLiveSearchResponse(
                    name = model.username,
                    url = "$mainUrl/${model.username}",
                    type = TvType.Live,
                    fix = false
                ) {
                    posterUrl = "https://img.doppiocdn.live/thumbs/${model.snapshotTimestamp}/${model.streamName}"
                    lang = null
                }
            } ?: emptyList()
        } else {
            // offset parametresini page'e göre veriyoruz
            val turkishResponse =
                app.get("""$mainUrl/api/front/models?removeShows=true&recInFeatured=false&limit=$limit&offset=$offset&primaryTag=girls&filterGroupTags=[["${request.data}"]]&sortBy=stripRanking&parentTag=${request.data}&nic=true&byw=false&rcmGrp=A&rbCnGr=true&rbdCnGr=true&prxCnGr=true&iem=false&mvPrm=false&uniq=t2gqiu9zx7dpn3m6""")
                    .parsedSafe<Response>()

            Log.d("kraptor_StripChat", "turkishResponse = $turkishResponse")

            // ham API model sayısını al (null safe)
            val rawCount = turkishResponse?.models?.size ?: 0

            val mapped = turkishResponse?.models?.map { model ->
                eList.add(model.id.toInt())
                Log.d("kraptor_StripChat", "poster = ${model.previewUrlThumbSmall}")
                newLiveSearchResponse(
                    name = model.username,
                    url = "$mainUrl/${model.username}",
                    type = TvType.Live,
                    fix = false
                ) {
                    posterUrl = "https://img.doppiocdn.live/thumbs/${model.snapshotTimestamp}/${model.streamName}"
                    lang = null
                }
            } ?: emptyList()
            mapped
        }

        // excludeIdsMap güncelleme
        if (excludeIdsMap[request.data].isNullOrEmpty()) {
            excludeIdsMap[request.data] = mutableListOf()
        }
        excludeIdsMap[request.data]?.addAll(eList)

        // hasNext hesaplama: eğer API ham sonucu limit'ten küçükse -> son sayfa
        // NOT: Eğer filtreden sonra az öğe görünmesi sorun ise, burada rawCount'u kullanın.
        val hasNext = when {
            // Eğer responseList size kullanmak isterseniz: val visibleCount = responseList.size
            // Ancak doğru karar API'nin ham döndürdüğü count'a göre olmalı.
            // Burada basit kural:
            (responseList.size < limit && page == 0 && responseList.isNotEmpty()) -> false // optional
            else -> responseList.size >= limit
        }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = responseList,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select(".model-list-item-username").text()
        val href = mainUrl + this.select(".model-list-item-link").attr("href")
        val posterUrl = this.selectFirst(".image-background")?.attr("src")
        return newLiveSearchResponse(
            name = title,
            url = href,
            type = TvType.Live,
            fix = false
        ) {
            this.posterUrl = posterUrl
            lang = null
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val headers = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        val doc = app.get("$mainUrl/search/models/$query", headers = headers).document
        return doc.select(".model-list-item").map { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val headers = mutableMapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
        )
        val document = app.get(url, headers = headers).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
            .replace("| PornHoarder.tv", "")
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()


        return newLiveStreamLoadResponse(
            name = title,
            url = url,
            dataUrl = url
        ) {
            posterUrl = poster
            plot = description
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        try {
//            Log.d("kraptor_$name", "loadLinks start for: $data")

            // 1) Sayfa al
            val page = app.get(data).document

            // 2) Script bul (kısa ve net)
            val script = page.select("script").firstOrNull {
                val h = it.html()
                h.contains("window.__PRELOADED_STATE__") || h.contains("\"streamName\"") || h.contains("hlsStreamUrlTemplate")
            } ?: run {
//                Log.e("kraptor_$name", "preloaded script not found")
                return false
            }
            val scriptText = script.html().unescapeUnicode()

            // 3) streamName / host / template çıkar
            val streamName = scriptText.substringAfter("\"streamName\":\"", "").substringBefore("\",")
            val streamHost = scriptText.substringAfter("\"hlsStreamHost\":\"", "").substringBefore("\",")
            val hlsTemplate = scriptText.substringAfter("\"hlsStreamUrlTemplate\":\"", "").substringBefore("\",")

            if (streamName.isEmpty() || hlsTemplate.isEmpty()) {
//                Log.e("kraptor_$name", "Missing streamName or hlsTemplate")
                return false
            }

            val finalM3u8 = hlsTemplate.replace("{cdnHost}", streamHost).replace("{streamName}", streamName)
                .replace("{suffix}", "_auto")
//            Log.d("kraptor_$name", "final m3u8: $finalM3u8")

            // 4) master playlist al
            val masterText = app.get(finalM3u8, mapOf("Referer" to "https://www.stripchat.com/")).text
//            Log.d("kraptor_$name", "master length=${masterText.length}")

            // ---------------------
            // Basit yardımcı: son eşleşen PSCH/PKEY'i çıkar
            fun extractLastPschPkey(text: String): Pair<String, String> {
                // multiline, case-insensitive, group1 = version, group2 = pkey (her şey sonra)
                val re = Regex("(?m)^#EXT-X-MOUFLON:PSCH:([^:]+):(.+)\$", RegexOption.IGNORE_CASE)
                val matches = re.findAll(text).toList()
                if (matches.isEmpty()) return Pair("", "")
                val last = matches.last()
                val version = last.groupValues.getOrNull(1)?.trim() ?: ""
                val pkey = last.groupValues.getOrNull(2)?.trim() ?: ""
                return Pair(version, pkey)
            }

            val (masterPsch, masterPkey) = extractLastPschPkey(masterText)
//            Log.d("kraptor_$name", "master psch='$masterPsch' pkey='$masterPkey'")

            // 5) variant URL seç (ilk gerçek URL)
            val variantRaw = masterText.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: finalM3u8
//            Log.d("kraptor_$name", "variant raw = $variantRaw")

            // 6) query param ekleme (basit, güvenli)
            fun withQueryParams(url: String, params: Map<String, String>): String {
                if (params.isEmpty()) return url
                val base = if (url.contains("?")) "$url&" else "$url?"
                val q = params.filterValues { it.isNotEmpty() }.map { (k, v) ->
                    java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8")
                }.joinToString("&")
                return if (q.isEmpty()) url else base + q
            }

            // kullanacağımız psch/pkey -> önce master, sonra variant override edecek
            var activePsch = masterPsch
            var activePkey = masterPkey

            val variantUrl = withQueryParams(variantRaw, mapOf("psch" to activePsch, "pkey" to activePkey))
//            Log.d("kraptor_$name", "variant url (with master params) = $variantUrl")

            // 7) variant playlist al
            val variantText = app.get(variantUrl, mapOf("Referer" to finalM3u8)).text
//            Log.d("kraptor_$name", "variant length=${variantText.length}")

            // 8) variant içinden son PSCH/PKEY varsa onu kullan (override)
            val (variantPsch, variantPkey) = extractLastPschPkey(variantText)
            if (variantPsch.isNotEmpty() || variantPkey.isNotEmpty()) {
                activePsch = variantPsch.ifEmpty { activePsch }
                activePkey = variantPkey.ifEmpty { activePkey }
//                Log.d("kraptor_$name", "Overrode with variant psch='$variantPsch' pkey='$variantPkey'")
            }

            // 9) Eğer variant URL'ini baştan psch/pkey ile tekrar kullanmak istersen:
            val variantUrlWithFinal = withQueryParams(variantRaw, mapOf("psch" to activePsch, "pkey" to activePkey))

            // 10) decrypt / decode işlemi (send function olarak basit fetcher veriliyor)
            val decoded = decodeM3u8MouflonFilesFixed(variantText, { url, headers ->
                try {
                    app.get(url, headers ?: mapOf("Referer" to finalM3u8, "User-Agent" to "Mozilla/5.0")).text
                } catch (t: Throwable) {
                    null
                }
            }, context.cacheDir)

            // 11) proxy başlat ve registre et (senin mevcut SimpleProxyServer kullanımı gibi)
            val proxyPort = SimpleProxyServer.getEphemeralPort()
            val proxy = SimpleProxyServer(
                port = proxyPort,
                cacheDir = context.cacheDir,
                fetchFunction = { url, headers ->
                    try {
                        app.get(url, headers ?: mapOf()).text
                    } catch (t: Throwable) {
                        null
                    }
                }
            )
            val actualPort = proxy.start()
            val liveUrl = proxy.registerLiveStream(
                variantUrl = variantUrlWithFinal,
                baseUrl = variantUrlWithFinal.substringBeforeLast("/") + "/",
                psch = activePsch,
                pkey = activePkey,
                referer = finalM3u8
            )

            // 12) geri dön
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "$name Live",
                    url = liveUrl,
                    type = ExtractorLinkType.M3U8,
                    {
                        referer = finalM3u8
                        quality = Qualities.Unknown.value
                    }
                )
            )

//            Log.d("kraptor_$name", "loadLinks finished ok")
            return true

        } catch (e: Exception) {
//            Log.e("kraptor_$name", "loadLinks failed: ${e.message}", e)
            return false
        }
    }


    data class Model(
        @JsonProperty("hlsPlaylist") val hlsPlaylist: String = "",
        @JsonProperty("id") val id: String = "",
        @JsonProperty("previewUrlThumbSmall") val previewUrlThumbSmall: String = "",
        @JsonProperty("username") val username: String = "",
        @JsonProperty("streamName") val streamName: String = "",
        @JsonProperty("snapshotTimestamp") val snapshotTimestamp: String = ""

    )

    data class Response(
        @JsonProperty("models") val models: List<Model> = arrayListOf()
    )
}

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}

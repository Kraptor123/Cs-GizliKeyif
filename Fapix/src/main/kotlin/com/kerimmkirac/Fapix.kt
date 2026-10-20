// ! Bu araç @kerimmkirac tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kerimmkirac

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import java.io.File

class Fapix(private val cacheDir: File) : MainAPI() {
    override var mainUrl              = "https://fapix.net"
    override var name                 = "Fapix"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "${mainUrl}/videos/latest"      to "All Porn Videos",
        "${mainUrl}/bro-sis"   to "Step Sister Porn Videos",
        "${mainUrl}/mother-and-son"  to "StepMom Porn Videos",
        "${mainUrl}/rus-porn-2025"  to "Russian Porn Videos",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val document = app.get("${request.data}?page=$page").document
    val home = document.select("div.video.trailer").mapNotNull { it.toMainPageResult() }

    return newHomePageResponse(
        list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
        ),
        hasNext = home.isNotEmpty()
    )
}

private fun Element.toMainPageResult(): SearchResponse? {
    val anchor = this.selectFirst("a") ?: return null
    val href = fixUrlNull(anchor.attr("href")) ?: return null
    val title = anchor.selectFirst("div.title")?.attr("title")?.trim()
        ?: anchor.attr("alt")?.trim()
        ?: return null
    val posterUrl = fixUrlNull(anchor.selectFirst("img")?.attr("src"))


    return newMovieSearchResponse(title, href, TvType.NSFW) {
        this.posterUrl = posterUrl


    }
}


    override suspend fun search(query: String, page: Int): SearchResponseList {
        val results = FapixSearchIndex.search(query, cacheDir).mapNotNull { entry ->
            val href = fixUrlNull("$mainUrl/video/${entry.id}") ?: return@mapNotNull null
            val posterUrl = fixUrlNull("$mainUrl/images/${entry.id}.webp")

            newMovieSearchResponse(entry.title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }

        return newSearchResponseList(results, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1[itemprop=name]")?.ownText()?.trim() ?: return null


        val poster = fixUrlNull(
            document.selectFirst("video#player")?.attr("poster")
                ?: document.selectFirst("video#player")?.attr("data-poster")
        )

        val tags = document.select("span.info-row a.button").map { it.text().trim() }.take(5)


        val actors = document.select("a.tag-modifier[itemprop=actor]").map {
            val name = it.selectFirst("span[itemprop=name]")?.text()?.trim() ?: return@map null
            val image = fixUrlNull(it.selectFirst("img")?.attr("src"))
            Actor(name, image)
        }.filterNotNull()


        val recommendations =
            document.select("div.video.htrailer").mapNotNull { it.toRecommendationResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags

            this.recommendations = recommendations
            addActors(actors)
        }
    }


    private fun Element.toRecommendationResult(): SearchResponse? {
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val title = this.selectFirst("div.title")?.attr("title")?.trim() ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val document = app.get(data).document
    val sourceUrl = document.selectFirst("video#player source")?.attr("src") ?: return false
    val finalUrl = app.get(
        sourceUrl,
        headers = mapOf("Referer" to mainUrl),
        allowRedirects = false
    ).headers["Location"] ?: sourceUrl

    callback.invoke(
        newExtractorLink(
            name = "Fapix",
            source = "Fapix",
            url = finalUrl,

            type = if (finalUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
        ){
            this.referer = mainUrl
            this.quality = Qualities.Unknown.value
        }
    )

    return true
}
}

private data class FapixIndexEntry(val id: Int, val title: String)

private data class FapixModelIndex(val name: String, val ids: List<Int>)

private data class FapixParsedIndex(val videos: List<FapixIndexEntry>, val models: List<FapixModelIndex>)

private object FapixSearchIndex {
    private const val INDEX_URL   = "https://fapix.net/sitemap/search.js"
    private const val CACHE_NAME  = "fapix_search_index_v2.tsv"
    private const val MAX_AGE_MS  = 12L * 60L * 60L * 1000L
    private const val MAX_RESULTS = 120
    private const val VIDEO_LINE  = "V\t"
    private const val MODEL_LINE  = "M\t"

    suspend fun search(query: String, cacheDir: File): List<FapixIndexEntry> {
        val needle = query.trim().uppercase()
        if (needle.isEmpty()) return emptyList()

        val indexFile = ensureIndex(cacheDir) ?: return emptyList()

        val modelIds = HashSet<Int>()
        indexFile.bufferedReader().use { reader ->
            val lines = reader.lineSequence().iterator()
            while (lines.hasNext()) {
                val line = lines.next()
                if (!line.startsWith(MODEL_LINE)) continue
                val nameEnd = line.indexOf('\t', MODEL_LINE.length)
                if (nameEnd < 0) continue
                if (!line.substring(MODEL_LINE.length, nameEnd).contains(needle, ignoreCase = true)) continue
                line.substring(nameEnd + 1).split(',').forEach { raw ->
                    raw.toIntOrNull()?.let { modelIds.add(it) }
                }
            }
        }

        val results = mutableListOf<FapixIndexEntry>()
        indexFile.bufferedReader().use { reader ->
            val lines = reader.lineSequence().iterator()
            while (lines.hasNext() && results.size < MAX_RESULTS) {
                val line = lines.next()
                if (!line.startsWith(VIDEO_LINE)) continue
                val idEnd = line.indexOf('\t', VIDEO_LINE.length)
                if (idEnd < 0) continue
                val id = line.substring(VIDEO_LINE.length, idEnd).toIntOrNull() ?: continue
                val title = line.substring(idEnd + 1)
                if (title.contains(needle, ignoreCase = true) || modelIds.contains(id)) {
                    results.add(FapixIndexEntry(id, title))
                }
            }
        }

        return results
    }

    private suspend fun ensureIndex(cacheDir: File): File? {
        val indexFile = File(cacheDir, CACHE_NAME)
        val isFresh   = indexFile.exists() && (System.currentTimeMillis() - indexFile.lastModified()) < MAX_AGE_MS
        if (isFresh) return indexFile

        return try {
            val js      = app.get(INDEX_URL).text
            val index   = parseIndex(js)
            writeIndex(indexFile, index)
            Log.d("Fapix", "index rebuilt videos=${index.videos.size} models=${index.models.size}")
            indexFile
        } catch (e: Exception) {
            Log.d("Fapix", "index failed: ${e.message}")
            if (indexFile.exists()) indexFile else null
        }
    }

    private fun parseIndex(js: String): FapixParsedIndex {
        val marker   = "search=["
        val start    = js.indexOf(marker)
        val elements = if (start < 0) emptyList() else topLevelElements(js, start + marker.length)

        val titles      = parseStringList(elements.getOrNull(0) ?: "[]")
        val ids         = parseIntList(elements.getOrNull(1) ?: "[]")
        val modelNames  = parseStringList(elements.getOrNull(2) ?: "[]")
        val modelVideos = parseIntListList(elements.getOrNull(5) ?: "[]")

        val videos = mutableListOf<FapixIndexEntry>()
        for (i in titles.indices) {
            val id = ids.getOrNull(i) ?: continue
            videos.add(FapixIndexEntry(id, cleanTitle(titles[i])))
        }

        val models = mutableListOf<FapixModelIndex>()
        for (j in modelNames.indices) {
            val modelIds = modelVideos.getOrNull(j) ?: continue
            models.add(FapixModelIndex(cleanTitle(modelNames[j]), modelIds.mapNotNull { ids.getOrNull(it) }))
        }

        return FapixParsedIndex(videos, models)
    }

    private fun writeIndex(indexFile: File, index: FapixParsedIndex) {
        val tempFile = File(indexFile.parentFile, "$CACHE_NAME.tmp")
        tempFile.bufferedWriter().use { writer ->
            index.videos.forEach { entry ->
                writer.write("$VIDEO_LINE${entry.id}\t${entry.title}\n")
            }
            index.models.forEach { model ->
                writer.write("$MODEL_LINE${model.name}\t${model.ids.joinToString(",")}\n")
            }
        }
        if (indexFile.exists()) indexFile.delete()
        tempFile.renameTo(indexFile)
    }

    private fun cleanTitle(raw: String): String {
        val decoded = Parser.unescapeEntities(raw, false).replace("\t", " ").replace("\n", " ").trim()
        return decoded.toSentenceCase()
    }

    private fun String.toSentenceCase(): String {
        val builder = StringBuilder(length)
        var firstLetter = true
        for (i in indices) {
            val c = this[i]
            when {
                c.isLetter() && firstLetter -> {
                    builder.append(c.uppercaseChar())
                    firstLetter = false
                }
                c.equals('i', ignoreCase = true) && i > 0 && !this[i - 1].isLetter() && (i + 1 == length || !this[i + 1].isLetter()) -> {
                    builder.append('I')
                }
                else -> builder.append(c.lowercaseChar())
            }
        }
        return builder.toString()
    }

    private fun topLevelElements(text: String, start: Int): List<String> {
        val elements = mutableListOf<String>()
        var i = start
        while (i < text.length) {
            val c = text[i]
            when {
                c == '[' -> {
                    val end = closingBracket(text, i)
                    if (end < 0) break
                    elements.add(text.substring(i, end + 1))
                    i = end + 1
                }
                c == ',' -> i++
                c == ']' -> return elements
                c.isWhitespace() -> i++
                else -> {
                    val end = valueEnd(text, i)
                    elements.add(text.substring(i, end))
                    i = end
                }
            }
        }
        return elements
    }

    private fun valueEnd(text: String, start: Int): Int {
        val c = text[start]
        if (c == '\'' || c == '"') return stringEnd(text, start) + 1
        var i = start
        while (i < text.length && (text[i].isDigit() || text[i] == '-' || text[i] == '.')) i++
        return if (i == start) start + 1 else i
    }

    private fun closingBracket(text: String, start: Int): Int {
        var depth = 0
        var i = start
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\'' || c == '"' -> i = stringEnd(text, i)
                c == '[' -> depth++
                c == ']' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }

    private fun stringEnd(text: String, start: Int): Int {
        val quote = text[start]
        var i = start + 1
        while (i < text.length) {
            val c = text[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == quote) return i
            i++
        }
        return text.length - 1
    }

    private fun parseStringList(arrayText: String): List<String> {
        val values  = mutableListOf<String>()
        val builder = StringBuilder()
        var i = 0
        while (i < arrayText.length) {
            val c = arrayText[i]
            if (c != '\'' && c != '"') {
                i++
                continue
            }
            builder.setLength(0)
            i++
            while (i < arrayText.length) {
                val ch = arrayText[i]
                if (ch == '\\' && i + 1 < arrayText.length) {
                    builder.append(arrayText[i + 1])
                    i += 2
                    continue
                }
                if (ch == c) {
                    i++
                    break
                }
                builder.append(ch)
                i++
            }
            values.add(builder.toString())
        }
        return values
    }

    private fun parseIntList(arrayText: String): List<Int> {
        val values = mutableListOf<Int>()
        var i = 0
        while (i < arrayText.length) {
            val c = arrayText[i]
            if (c.isDigit() || c == '-') {
                var j = i
                if (c == '-') j++
                while (j < arrayText.length && arrayText[j].isDigit()) j++
                arrayText.substring(i, j).toIntOrNull()?.let { values.add(it) }
                i = j
            } else {
                i++
            }
        }
        return values
    }

    private fun parseIntListList(arrayText: String): List<List<Int>> {
        val lists = mutableListOf<List<Int>>()
        var i = if (arrayText.startsWith("[")) 1 else 0
        while (i < arrayText.length) {
            if (arrayText[i] == '[') {
                val end = closingBracket(arrayText, i)
                if (end < 0) break
                lists.add(parseIntList(arrayText.substring(i, end + 1)))
                i = end + 1
            } else {
                i++
            }
        }
        return lists
    }
}

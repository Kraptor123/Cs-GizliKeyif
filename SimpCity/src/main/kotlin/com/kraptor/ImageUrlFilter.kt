package com.kraptor

object ImageUrlFilter {

    private val THUMBNAIL_PATTERNS = listOf(
        Regex("""/thumbnails?/""", RegexOption.IGNORE_CASE),
        Regex("""[?&](width|w)=\d{1,3}($|&)""", RegexOption.IGNORE_CASE),
        Regex("""[?&](height|h)=\d{1,3}($|&)""", RegexOption.IGNORE_CASE),
        Regex("""[?&]size=(thumb|small|tiny|mini|preview)""", RegexOption.IGNORE_CASE),
        Regex("""[/_-](thumb|thumbnail|small|tiny|mini|preview|poster|avatar)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""/\d{1,3}x\d{1,3}/"""),
        Regex("""[?&]q=\d{1,2}($|&)"""),
    )

    private val FULL_QUALITY_PATTERNS = listOf(
        Regex("""[/_-](full|original|large|raw|hires)\b""", RegexOption.IGNORE_CASE),
        Regex("""[?&](width|w)=\d{4,}""", RegexOption.IGNORE_CASE),
        Regex("""[?&]size=(full|original|large)""", RegexOption.IGNORE_CASE),
        Regex("""/originals?/""", RegexOption.IGNORE_CASE),
        Regex("""/full/""", RegexOption.IGNORE_CASE),
    )

    fun isLikelyThumbnail(url: String): Boolean {
        return THUMBNAIL_PATTERNS.any { it.containsMatchIn(url) }
    }

    fun isLikelyFullQuality(url: String): Boolean {
        return FULL_QUALITY_PATTERNS.any { it.containsMatchIn(url) }
    }

    fun filterFullQuality(urls: List<String>): List<String> {
        return urls
            .filterNot { isLikelyThumbnail(it) }
            .distinctBy { it.substringBefore("?").substringBefore("#") }
    }

    fun upgradeToFullQuality(url: String): String {
        var upgraded = url
        upgraded = upgraded.replace(Regex("""[._-](md|th)\.""", RegexOption.IGNORE_CASE), ".")
        upgraded = upgraded.replace(
            Regex("""/thumbnails?/""", RegexOption.IGNORE_CASE),
            "/originals/"
        )
        upgraded = upgraded.replace(
            Regex("""[?&](width|w|height|h|size)=\S+?(&|$)"""),
            ""
        )
        upgraded = upgraded.replace(
            Regex("""[/_-](thumb|thumbnail|small|tiny|mini)""", RegexOption.IGNORE_CASE),
            "/original"
        )
        return upgraded
    }

    fun toThumbnail(url: String): String {
        if (url.contains(".th.") || url.contains(".md.")) return url
        val ext = url.substringAfterLast(".", "")
        if (ext.lowercase() in listOf("jpg", "jpeg", "png", "webp")) {
            return url.substringBeforeLast(".") + ".th." + ext
        }
        return url
    }
}

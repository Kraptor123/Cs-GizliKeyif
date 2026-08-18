// ! This Extension Made By @kraptor for GizliKeyif

package com.kraptor

import com.lagradost.api.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Max : MainAPI() {
    override var mainUrl = "https://max.porn"
    override var name = "Max"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val tag = "gizlikeyif_${name}"

    private val allChannels = listOf(
        "$mainUrl/channels/puba/" to "Puba",
        "$mainUrl/channels/brazzers/" to "Brazzers",
        "$mainUrl/channels/adult-prime/" to "Adult Prime",
        "$mainUrl/channels/realitykings/" to "Reality Kings",
        "$mainUrl/channels/bang/" to "Bang!",
        "$mainUrl/channels/naughty-america/" to "Naughty America",
        "$mainUrl/channels/scoreland/" to "Scoreland",
        "$mainUrl/channels/bangbros/" to "Bang Bros",
        "$mainUrl/channels/premium-gfs/" to "Premium GFs",
        "$mainUrl/channels/teamskeet/" to "Team Skeet",
        "$mainUrl/channels/love-home-porn/" to "Love Home Porn",
        "$mainUrl/channels/jeffs-models/" to "Jeffs Models",
        "$mainUrl/channels/evil-angel/" to "Evil Angel",
        "$mainUrl/channels/sexyhub/" to "Sexy Hub",
        "$mainUrl/channels/the-habib-show/" to "The Habib Show",
        "$mainUrl/channels/adulttime/" to "Adult Time",
        "$mainUrl/channels/nubiles-porn/" to "Nubiles Porn",
        "$mainUrl/channels/familystrokes/" to "Family Strokes",
        "$mainUrl/channels/private/" to "Private",
        "$mainUrl/channels/clubseventeen/" to "Club Sweethearts",
        "$mainUrl/channels/perv-milfs-and-teens/" to "Perv Milfs And Teens",
        "$mainUrl/channels/zizi-vids/" to "Zizi Vids",
        "$mainUrl/channels/mofos/" to "Mofos",
        "$mainUrl/channels/caribbean-com/" to "Caribbean.com",
        "$mainUrl/channels/karups/" to "Karups",
        "$mainUrl/channels/japan-hdv/" to "Japan HDV",
        "$mainUrl/channels/av-69/" to "AV 69",
        "$mainUrl/channels/box-of-porn/" to "Box Of Porn",
        "$mainUrl/channels/grooby-girls/" to "Grooby Girls",
        "$mainUrl/channels/black-cat-production/" to "Black Cat Production",
        "$mainUrl/channels/nubiles-net/" to "Nubiles.net",
        "$mainUrl/channels/shagging-moms/" to "Shagging Moms",
        "$mainUrl/channels/magic-asian-pussy/" to "Magic Asian Pussy",
        "$mainUrl/channels/my-dirty-hobby/" to "My Dirty Hobby",
        "$mainUrl/channels/atk-girlfriends/" to "ATK Girlfriends",
        "$mainUrl/channels/puffy-network/" to "Puffy Network",
        "$mainUrl/channels/porn-world/" to "Porn World",
        "$mainUrl/channels/cherry-pimps/" to "Cherry Pimps",
        "$mainUrl/channels/yanks/" to "Yanks",
        "$mainUrl/channels/jules-jordan/" to "Jules Jordan",
        "$mainUrl/channels/atk-hairy/" to "ATK Hairy",
        "$mainUrl/channels/immoral-live/" to "Immoral live",
        "$mainUrl/channels/spizoo/" to "Spizoo",
        "$mainUrl/channels/mature-nl/" to "Mature NL",
        "$mainUrl/channels/chickpass-adult-network/" to "ChickPass Adult Network",
        "$mainUrl/channels/interracial-pass/" to "Interracial Pass",
        "$mainUrl/channels/hush-pass/" to "Hush Pass",
        "$mainUrl/channels/wankz/" to "Wankz",
        "$mainUrl/channels/pervcity/" to "Perv City",
        "$mainUrl/label/survy-girl-auditions/" to "Сurvy Girl Auditions",
        "$mainUrl/label/shickpass/" to "Сhickpass",
        "$mainUrl/label/amateurs-series2/" to "Аmateurs Series",
        "$mainUrl/label/szech-saravan/" to "Сzech Сaravan",
        "$mainUrl/label/av-69/" to "AV 69",
        "$mainUrl/label/atk-girlfriends/" to "ATK Girlfriends",
        "$mainUrl/label/atk-hairy/" to "ATK Hairy",
        "$mainUrl/label/avtits/" to "AVTits",
        "$mainUrl/label/atk-exotics/" to "ATK Exotics",
        "$mainUrl/label/avstockings/" to "AVStockings",
        "$mainUrl/label/avidol-z/" to "Avidol Z",
        "$mainUrl/label/anilos/" to "Anilos",
        "$mainUrl/label/asiam/" to "AsiaM",
        "$mainUrl/label/atk-galleria/" to "ATK Galleria",
        "$mainUrl/label/avanal/" to "AVAnal",
        "$mainUrl/label/anal-introductions/" to "Anal Introductions",
        "$mainUrl/label/aunt-judy-s/" to "Aunt Judy's",
        "$mainUrl/label/adult-time/" to "Adult Time",
        "$mainUrl/label/analized/" to "Analized",
        "$mainUrl/label/brazzers-exxtra/" to "Brazzers Exxtra",
        "$mainUrl/label/box-of-porn/" to "Box Of Porn",
        "$mainUrl/label/big-tits/" to "Big Tits",
        "$mainUrl/label/blacked-com/" to "Blacked.Com",
        "$mainUrl/label/bratty-sis/" to "Bratty Sis",
        "$mainUrl/label/bambulax/" to "Bambulax",
        "$mainUrl/label/bonga-cams/" to "Bonga Cams",
        "$mainUrl/label/blacked-raw/" to "Blacked Raw",
        "$mainUrl/label/black-tgirls/" to "Black TGirls",
        "$mainUrl/label/bound-heat/" to "Bound Heat",
        "$mainUrl/label/bellesa-films/" to "Bellesa Films",
        "$mainUrl/label/backroom-casting-couch/" to "Backroom Casting Couch",
        "$mainUrl/label/bollywood-nudes-hd/" to "Bollywood Nudes HD",
        "$mainUrl/label/bam-visions/" to "Bam Visions",
        "$mainUrl/label/bigboob-bundle/" to "BigBoob Bundle",
        "$mainUrl/label/caribbeancom/" to "Caribbeancom",
        "$mainUrl/label/club-sweethearts/" to "Club Sweethearts",
        "$mainUrl/label/chickpass-adult-network/" to "ChickPass Adult Network",
        "$mainUrl/label/cam-soda/" to "Cam Soda",
        "$mainUrl/label/cherry-pimps/" to "Cherry Pimps",
        "$mainUrl/label/chickpass-amateurs/" to "ChickPass Amateurs",
        "$mainUrl/label/cam4/" to "CAM4",
        "$mainUrl/label/cuck-hunter/" to "Cuck Hunter",
        "$mainUrl/label/charlee-chase/" to "Charlee Chase",
        "$mainUrl/label/casual-teen-sex/" to "Casual Teen Sex",
        "$mainUrl/label/czech-sex-casting/" to "Czech Sex Casting",
        "$mainUrl/label/clubtug-com/" to "Club Tug",
        "$mainUrl/label/creampie-thais/" to "Creampie Thais",
        "$mainUrl/label/college-rules/" to "College Rules",
        "$mainUrl/label/culioneros/" to "Culioneros",
        "$mainUrl/label/dream-tranny/" to "Dream Tranny",
        "$mainUrl/label/dreamgirls-members/" to "DreamGirls Members",
        "$mainUrl/label/daddy4k/" to "Daddy4k",
        "$mainUrl/label/desperate-amateurs/" to "Desperate Amateurs",
        "$mainUrl/label/dadcrush/" to "Dad Crush",
        "$mainUrl/label/debt4k/" to "Debt4k",
        "$mainUrl/label/down-for-bbc/" to "Down For BBC",
        "$mainUrl/label/devil-s-film/" to "Devil's Film",
        "$mainUrl/label/distorded/" to "Distorded",
        "$mainUrl/label/defloration-tv/" to "Defloration TV",
        "$mainUrl/label/digitalplayground/" to "Digital Playground",
        "$mainUrl/label/deeper/" to "Deeper",
        "$mainUrl/label/deutschland-porno/" to "Deutschland Porno",
        "$mainUrl/label/dane-jones/" to "Dane Jones",
        "$mainUrl/label/dtf-sluts/" to "DTF Sluts",
        "$mainUrl/label/erotikvonbenan/" to "Erotikvonbenan",
        "$mainUrl/label/extreme-movie-pass/" to "Extreme Movie Pass",
        "$mainUrl/label/erotic-female-domination/" to "Erotic Female Domination",
        "$mainUrl/label/excogi/" to "ExCoGi",
        "$mainUrl/label/erotic-planet/" to "Erotic Planet",
        "$mainUrl/label/eros-exotica-hd/" to "Eros Exotica HD",
        "$mainUrl/label/eleganxia/" to "Eleganxia",
        "$mainUrl/label/ed-powers/" to "Ed Powers",
        "$mainUrl/label/erotiquetvlive/" to "ErotiqueTVLive",
        "$mainUrl/label/elegant-raw/" to "Elegant Raw",
        "$mainUrl/label/exxxtrasmall/" to "Exxxtra Small",
        "$mainUrl/label/eroticax/" to "Erotica X",
        "$mainUrl/label/ebony-thots/" to "Ebony Thots",
        "$mainUrl/label/erito/" to "Erito",
        "$mainUrl/label/enjoyx/" to "Enjoyx",
        "$mainUrl/label/ferame/" to "Ferame",
        "$mainUrl/label/fakings/" to "FaKings",
        "$mainUrl/label/fakehub/" to "Fakehub",
        "$mainUrl/label/fap-house/" to "Fap House",
        "$mainUrl/label/fake-taxi/" to "Fake Taxi",
        "$mainUrl/label/filthy-kings/" to "Filthy Kings",
        "$mainUrl/label/familystrokes/" to "Family Strokes",
        "$mainUrl/label/femout/" to "Femout",
        "$mainUrl/label/fellucia-blow-hd/" to "Fellucia Blow HD",
        "$mainUrl/label/female-muscle-network/" to "Female Muscle Network",
        "$mainUrl/label/fake-hostel/" to "Fake Hostel",
        "$mainUrl/label/freeuse/" to "FreeUse",
        "$mainUrl/label/first-class-pov/" to "First Class POV",
        "$mainUrl/label/first-anal-quest/" to "First Anal Quest",
        "$mainUrl/label/familyxxx/" to "FAMILYxxx",
        "$mainUrl/label/grooby-girls/" to "Grooby Girls",
        "$mainUrl/label/girlsway/" to "Girls Way",
        "$mainUrl/label/gang-av/" to "Gang AV",
        "$mainUrl/label/golden-sluts/" to "Golden Sluts",
        "$mainUrl/label/girls-out-west/" to "Girls Out West",
        "$mainUrl/label/girlfriends-films/" to "Girlfriends Films",
        "$mainUrl/label/gs-porn/" to "GS Porn",
        "$mainUrl/label/gangbang-creampie/" to "Gangbang Creampie",
        "$mainUrl/label/gramps-on-teens/" to "Gramps On Teens",
        "$mainUrl/label/granny-bet/" to "Granny Bet",
        "$mainUrl/label/grandmams/" to "Grandmams",
        "$mainUrl/label/glory-hole-secrets/" to "Glory Hole Secrets",
        "$mainUrl/label/grandma-friends/" to "Grandma Friends",
        "$mainUrl/label/girlz-lust/" to "Girlz Lust",
        "$mainUrl/label/gilf-milf/" to "Gilf Milf",
        "$mainUrl/label/hey-milf/" to "Hey Milf",
        "$mainUrl/label/hush-pass/" to "Hush Pass",
        "$mainUrl/label/hunt4k/" to "Hunt4k",
        "$mainUrl/label/hairy-av/" to "Hairy AV",
        "$mainUrl/label/homegrown-video/" to "Homegrown Video",
        "$mainUrl/label/hot-guys-fuck/" to "Hot Guys Fuck",
        "$mainUrl/label/hardx/" to "Hard X",
        "$mainUrl/label/hormone-tokyo/" to "Hormone Tokyo",
        "$mainUrl/label/hardcore-japanese-gfs/" to "Hardcore Japanese GFs",
        "$mainUrl/label/hookup-hotshot/" to "Hookup Hotshot",
        "$mainUrl/label/hot-wife-xxx/" to "Hot Wife XXX",
        "$mainUrl/label/hot-hot-films/" to "Hot Hot Films",
        "$mainUrl/label/hussie-pass/" to "Hussie Pass",
        "$mainUrl/label/house-of-fyre/" to "House of Fyre",
        "$mainUrl/label/hairy-coochies/" to "Hairy Coochies",
        "$mainUrl/label/interracial-pass/" to "Interracial Pass",
        "$mainUrl/label/immorallive/" to "Immoral live",
        "$mainUrl/label/its-pov/" to "Its POV",
        "$mainUrl/label/its-just-sex/" to "Its Just Sex",
        "$mainUrl/label/i-have-a-wife/" to "I Have a Wife",
        "$mainUrl/label/i-buy-gfs/" to "I Buy GFs",
        "$mainUrl/label/inka-sex/" to "Inka Sex",
        "$mainUrl/label/interracial-vision/" to "Interracial Vision",
        "$mainUrl/label/immoral-family/" to "Immoral Family",
        "$mainUrl/label/innocent-high/" to "Innocent High",
        "$mainUrl/label/i-know-that-girl/" to "I Know That Girl",
        "$mainUrl/label/i-kiss-girls/" to "I Kiss Girls",
        "$mainUrl/label/immoral-pov/" to "Immoral POV",
        "$mainUrl/label/its-cleo-live/" to "Its Cleo Live",
        "$mainUrl/label/intimate-pov/" to "Intimate POV",
        "$mainUrl/label/jav-hd/" to "Jav HD",
        "$mainUrl/label/jeffs-models/" to "Jeffs Models",
        "$mainUrl/label/jules-jordan/" to "Jules Jordan",
        "$mainUrl/label/japan-lust/" to "Japan Lust",
        "$mainUrl/label/japan-hdv/" to "Japan HDV",
        "$mainUrl/label/julia-ann-live/" to "Julia Ann Live",
        "$mainUrl/label/jays-pov/" to "Jays POV",
        "$mainUrl/label/japbliss/" to "JapBliss",
        "$mainUrl/label/joi-babes/" to "JOI Babes",
        "$mainUrl/label/jonni-darkko/" to "Jonni Darkko",
        "$mainUrl/label/james-deen/" to "James Deen",
        "$mainUrl/label/japan-uncensored/" to "Japan Uncensored",
        "$mainUrl/label/jerkaoke/" to "Jerkaoke",
        "$mainUrl/label/javhub/" to "JAVHub",
        "$mainUrl/label/jerky-girls/" to "Jerky Girls",
        "$mainUrl/label/karups-older-women/" to "Karup's Older Women",
        "$mainUrl/label/karup-s-private-collection/" to "Karup's Private Collection",
        "$mainUrl/label/karup-s-hometown-amateurs/" to "Karup's Hometown Amateurs",
        "$mainUrl/label/kings-of-fetish/" to "Kings Of Fetish",
        "$mainUrl/label/killergram/" to "Killergram",
        "$mainUrl/label/kinky-spa/" to "Kinky Spa",
        "$mainUrl/label/knocked-up-sluts/" to "Knocked Up Sluts",
        "$mainUrl/label/kissing-hd/" to "Kissing HD",
        "$mainUrl/label/kinky-family/" to "Kinky Family",
        "$mainUrl/label/kumalott/" to "Kumalott",
        "$mainUrl/label/k-mib/" to "K MIB",
        "$mainUrl/label/karups-pov/" to "Karups POV",
        "$mainUrl/label/karups/" to "Karups",
        "$mainUrl/label/kings-of-fetish-big-boobs/" to "Kings Of Fetish Big Boobs",
        "$mainUrl/label/kink/" to "Kink",
        "$mainUrl/label/love-home-porn/" to "Love Home Porn",
        "$mainUrl/label/lethal-pass/" to "Lethal Pass",
        "$mainUrl/label/life-selector/" to "Life Selector",
        "$mainUrl/label/ltg-sex-movies/" to "LTG Sex Movies",
        "$mainUrl/label/lethal-hardcore/" to "Lethal Hardcore",
        "$mainUrl/label/lost-bets-games/" to "Lost Bets Games",
        "$mainUrl/label/lesbea/" to "Lesbea",
        "$mainUrl/label/loan4k/" to "Loan4k",
        "$mainUrl/label/la-new-girl/" to "LA New Girl",
        "$mainUrl/label/lemon-juice/" to "Lemon Juice",
        "$mainUrl/label/let-sdoeit/" to "Let'sDoeIt",
        "$mainUrl/label/latina-fuck-tour/" to "Latina Fuck Tour",
        "$mainUrl/label/love-her-feet/" to "Love Her Feet",
        "$mainUrl/label/lustery/" to "Lustery",
        "$mainUrl/label/les-worship/" to "Les Worship",
        "$mainUrl/label/magic-asian-pussy/" to "Magic Asian Pussy",
        "$mainUrl/label/mydirtyhobby/" to "mydirtyhobby",
        "$mainUrl/label/mofos/" to "Mofos",
        "$mainUrl/label/mature-nl/" to "Mature NL",
        "$mainUrl/label/mmm100/" to "MMM100",
        "$mainUrl/label/mommy-s-girl/" to "Mommy's Girl",
        "$mainUrl/label/my-tiny-dick/" to "My Tiny Dick",
        "$mainUrl/label/my-pervy-family/" to "My Pervy Family",
        "$mainUrl/label/my-friend-s-hot-mom/" to "My Friend's Hot Mom",
        "$mainUrl/label/massage-rooms/" to "Massage Rooms",
        "$mainUrl/label/metart-x/" to "MetArt X",
        "$mainUrl/label/milf-bundle/" to "MILF Bundle",
        "$mainUrl/label/mom-xxx/" to "Mom XXX",
        "$mainUrl/label/milf-granny-store/" to "MILF &amp; GRANNY STORE",
        "$mainUrl/label/momsteachsex/" to "Moms Teach Sex",
        "$mainUrl/label/nubiles-net/" to "Nubiles.net",
        "$mainUrl/label/new-sensations/" to "New Sensations",
        "$mainUrl/label/nubile-films/" to "Nubile Films",
        "$mainUrl/label/naughty-mag/" to "Naughty Mag",
        "$mainUrl/label/netgirl/" to "Net Girl",
        "$mainUrl/label/naughty-america/" to "Naughty America",
        "$mainUrl/label/naughty-office/" to "Naughty Office",
        "$mainUrl/label/nookies/" to "Nookies",
        "$mainUrl/label/naughty-compilations/" to "Naughty Compilations",
        "$mainUrl/label/nubiles-porn/" to "Nubiles Porn",
        "$mainUrl/label/nude-yoga-porn/" to "Nude Yoga Porn",
        "$mainUrl/label/nippon-hd/" to "Nippon HD",
        "$mainUrl/label/nylon-up/" to "Nylon Up",
        "$mainUrl/label/neighbor-affair/" to "Neighbor Affair",
        "$mainUrl/label/nucosplay/" to "NuCosplay",
        "$mainUrl/label/only-3x/" to "Only 3X",
        "$mainUrl/label/oldje/" to "Oldje",
        "$mainUrl/label/old4k/" to "Old4k",
        "$mainUrl/label/out-of-the-family/" to "Out Of The Family",
        "$mainUrl/label/onlytarts/" to "OnlyTarts",
        "$mainUrl/label/oops-family/" to "Oops Family",
        "$mainUrl/label/onlyteenblowjobs/" to "Only Teen Blowjobs",
        "$mainUrl/label/oldnanny/" to "OldNanny",
        "$mainUrl/label/only-taboo/" to "Only Taboo",
        "$mainUrl/label/oye-loca/" to "Oye Loca",
        "$mainUrl/label/old-goes-young/" to "Old Goes Young",
        "$mainUrl/label/omg-big-boobs/" to "Omg Big Boobs",
        "$mainUrl/label/omg-i-squirted/" to "OMG I Squirted",
        "$mainUrl/label/office-pov/" to "Office POV",
        "$mainUrl/label/over40handjobs-com/" to "Over 40 Handjobs",
        "$mainUrl/label/puba/" to "PUBA",
        "$mainUrl/label/premium-gfs/" to "Premium GFs",
        "$mainUrl/label/perv-milfs-and-teens/" to "Perv Milfs And Teens",
        "$mainUrl/label/perv-city/" to "Perv City",
        "$mainUrl/label/pornstar-platinum/" to "Pornstar Platinum",
        "$mainUrl/label/porn-world/" to "Porn World",
        "$mainUrl/label/pascals-subsluts/" to "Pascals Subsluts",
        "$mainUrl/label/pooksi/" to "Pooksi",
        "$mainUrl/label/public-agent/" to "Public Agent",
        "$mainUrl/label/pure-taboo/" to "Pure Taboo",
        "$mainUrl/label/perv-mom/" to "Perv Mom",
        "$mainUrl/label/paco-paco/" to "Paco Paco",
        "$mainUrl/label/porn-art-x/" to "PORN ART X",
        "$mainUrl/label/penthouse/" to "Penthouse",
        "$mainUrl/label/property-sex/" to "Property Sex",
        "$mainUrl/label/queercrush/" to "QueerCrush",
        "$mainUrl/label/quente-club/" to "Quente Club",
        "$mainUrl/label/rk-prime/" to "RK Prime",
        "$mainUrl/label/rocco-siffredi/" to "Rocco Siffredi",
        "$mainUrl/label/rim4k/" to "Rim4k",
        "$mainUrl/label/raw-attack/" to "Raw Attack",
        "$mainUrl/label/rome-major/" to "Rome Major",
    )

    override val mainPage: List<MainPageData>
        get() = allChannels.shuffled().take(10).map { (url, name) ->
            MainPageData(name, url)
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page/").document
        val home =
            document.select("div.item:not(div.swiper-slide)").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(list = HomePageList(request.name, home, true))
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title = this.selectFirst("div.title")?.text() ?: return null
        Log.d(tag, "title = $title")
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        Log.d(tag, "href = $href")
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf("Referer" to "${mainUrl}/")
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val document = app.get("${mainUrl}/search/$query/$page/").document
        val searchAnswer = document.select("div.item").mapNotNull { it.toMainPageResult() }

        return newSearchResponseList(searchAnswer, hasNext = true)
    }


    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)


    override suspend fun load(url: String): LoadResponse? {
        Log.d(tag, "Load aşaması: $url")
        val document = app.get(url).document

        val title =
            document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description =
            document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("div.tags div.list a").map { it.text() }
        val recommendations =
            document.select("div.item:not(div.swiper-slide)").mapNotNull { it.toMainPageResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d(tag, "data = $data")
        val document = app.get(data).document

        val sources = document.selectFirst("source")?.attr("src") ?: ""

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = this.name,
                url = sources,
                type = ExtractorLinkType.M3U8,
                initializer = {
                    this.referer = "$mainUrl/"
                }
            ))

        return true
    }
}
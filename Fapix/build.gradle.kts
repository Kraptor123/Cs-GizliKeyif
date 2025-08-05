version = 2

cloudstream {
    authors     = listOf("kerimmkirac")
    language    = "en"
    description = "The only one site where you can filter videos by 4 tags at once for free. We have a lot of great videos, we're sure you'll like them"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("NSFW")
    iconUrl = "https://www.google.com/s2/favicons?domain=fapix.porn&sz=%size%"
}
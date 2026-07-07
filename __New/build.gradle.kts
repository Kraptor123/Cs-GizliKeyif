// ! This Extension Made By @Kraptor123 for GizliKeyif
version = 0

cloudstream {
    authors     = listOf("kraptor")
    language    = "en"
    description = "DESCRIPTIONDONOTFORGET"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    val domainIcon = "https://"

    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("NSFW")
    iconUrl = "https://www.google.com/s2/favicons?sz=64&domain=$domainIcon"
}
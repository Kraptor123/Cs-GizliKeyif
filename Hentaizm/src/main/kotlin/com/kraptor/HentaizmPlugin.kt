// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class HentaizmPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Hentaizm())
        registerExtractorAPI(VideoHu())
        registerExtractorAPI(CloudMailRu())
    }
}
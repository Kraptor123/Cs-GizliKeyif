// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class HentaizmPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Hentaizm())
        registerExtractorAPI(VideoHu())
        registerExtractorAPI(CloudMailRu())
    }
}
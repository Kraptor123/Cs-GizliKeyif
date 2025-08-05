// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class LiveCamRipsPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(LiveCamRips())
        registerExtractorAPI(Xpornium())
        registerExtractorAPI(Abstream())
        registerExtractorAPI(MixDrop())
    }
}
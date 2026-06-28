// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CamCapsPlugin: Plugin() {
    override fun load() {
        registerMainAPI(CamCaps())
        registerExtractorAPI(NowPlayExtractor())
        registerExtractorAPI(Vidello())
    }
}
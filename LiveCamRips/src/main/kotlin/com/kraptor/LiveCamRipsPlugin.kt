// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.MixDrop

@CloudstreamPlugin
class LiveCamRipsPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(LiveCamRips())
        registerExtractorAPI(Xpornium())
        registerExtractorAPI(Abstream())
        registerExtractorAPI(MixDrop())
    }
}
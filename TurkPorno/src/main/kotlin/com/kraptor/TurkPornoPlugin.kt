// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class TurkPornoPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(TurkPorno())
        registerExtractorAPI(VeevToExtractor())
        registerExtractorAPI(UpnsLiveExtractor())
    }
}
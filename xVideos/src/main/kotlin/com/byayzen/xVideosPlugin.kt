// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import android.content.Context
import com.byayzen.xVideosExtractor
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class xVideosPlugin: Plugin() {
    override fun load() {
        registerMainAPI(xVideos())
        registerExtractorAPI(xVideosExtractor())
    }
}
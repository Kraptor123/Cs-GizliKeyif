// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class XXXChinaPlugin: Plugin() {
    override fun load() {
        registerMainAPI(XXXChina())
        registerExtractorAPI(HQCloud())
        registerExtractorAPI(HQLinks())
        registerExtractorAPI(Vibuxer())
        registerExtractorAPI(Audinifer())
        registerExtractorAPI(TurbovidHLS())
    }
}
// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.extractors.EmturbovidExtractor
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class JavseenPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Javseen())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodStream())
        registerExtractorAPI(DoodDoply())
        registerExtractorAPI(DoodVideo())
        registerExtractorAPI(DoooodVideo())
        registerExtractorAPI(d000d())
        registerExtractorAPI(VidhideVIP())
        registerExtractorAPI(Voe())
        registerExtractorAPI(javclan())
        registerExtractorAPI(Javggvideo())
        registerExtractorAPI(EmturbovidExtractor())
        registerExtractorAPI(Javlion())
        registerExtractorAPI(swhoi())
        registerExtractorAPI(Javsw())
        registerExtractorAPI(Javmoon())
        registerExtractorAPI(Maxstream())
        registerExtractorAPI(MixDropis())
        registerExtractorAPI(Ds2Play())
        registerExtractorAPI(Streamwish())
        registerExtractorAPI(Vidhidepro())
        registerExtractorAPI(Streamhihi())
    }
}
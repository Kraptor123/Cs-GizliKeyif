// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.EmturbovidExtractor
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Voe

@CloudstreamPlugin
class JavGuruPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(JavGuru())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(DoodJav())
        registerExtractorAPI(d000d())
        registerExtractorAPI(VidhideVIP())
        registerExtractorAPI(Voe())
        registerExtractorAPI(StreamTape())
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
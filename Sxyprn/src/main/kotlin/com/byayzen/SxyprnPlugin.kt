// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.kraptor.VeevToExtractor
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.extractors.BigwarpArt
import com.lagradost.cloudstream3.extractors.BigwarpIO
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.StreamTapeNet
import com.lagradost.cloudstream3.extractors.StreamTapeXyz
import com.lagradost.cloudstream3.extractors.Streamup
import com.lagradost.cloudstream3.extractors.Vidguardto
import com.lagradost.cloudstream3.extractors.Vidguardto1
import com.lagradost.cloudstream3.extractors.Vidguardto2
import com.lagradost.cloudstream3.extractors.Vidguardto3
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.extractors.Voe1

@CloudstreamPlugin
class SxyprnPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Sxyprn())
        registerExtractorAPI(LuluStream())
        registerExtractorAPI(LuluVid())
        registerExtractorAPI(LuluVdo())
        registerExtractorAPI(VidNest())
        registerExtractorAPI(LuluPvp())
        registerExtractorAPI(Streamup())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(StreamTapeNet())
        registerExtractorAPI(StreamTapeXyz())
        registerExtractorAPI(DoodStream())
        registerExtractorAPI(DoodDoply())
        registerExtractorAPI(SaveFiles())
        registerExtractorAPI(BigwarpIO())
        registerExtractorAPI(BigwarpArt())
        registerExtractorAPI(Voe())
        registerExtractorAPI(Voe1())
        registerExtractorAPI(Vidguardto())
        registerExtractorAPI(Vidguardto1())
        registerExtractorAPI(Vidguardto2())
        registerExtractorAPI(Vidguardto3())
        registerExtractorAPI(VeevToExtractor())
    }
}
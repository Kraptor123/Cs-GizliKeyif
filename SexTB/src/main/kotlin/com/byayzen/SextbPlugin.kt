// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.*

@CloudstreamPlugin
class SextbPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Sextb())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(StreamTapeNet())
        registerExtractorAPI(StreamTapeXyz())
        registerExtractorAPI(Turboplayers())
        registerExtractorAPI(DoodStream())
        registerExtractorAPI(DoodDoply())
        registerExtractorAPI(DoodVideo())
        registerExtractorAPI(Ds2Play())
        registerExtractorAPI(d000d())
        registerExtractorAPI(VidHidePro())
        registerExtractorAPI(VidHidePro1())
        registerExtractorAPI(VidHidePro2())
        registerExtractorAPI(VidHidePro3())
        registerExtractorAPI(VidHidePro4())
        registerExtractorAPI(VidHidePro6())
        registerExtractorAPI(VidHidePro7())
        registerExtractorAPI(VidhideVIP())
        registerExtractorAPI(Javlion())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Smoothpre())
        registerExtractorAPI(Dhtpre())
        registerExtractorAPI(Peytonepre())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Dintezuvio())
        registerExtractorAPI(Streamwish())
        registerExtractorAPI(Streamhihi())
        registerExtractorAPI(Javsw())
        registerExtractorAPI(swhoi())
        registerExtractorAPI(Javmoon())
        registerExtractorAPI(MixDropis())
        registerExtractorAPI(Javclan())
        registerExtractorAPI(Maxstream())
        registerExtractorAPI(Javggvideo())
        registerExtractorAPI(EmturbovidExtractor())
        registerExtractorAPI(FileMoonSx())
        registerExtractorAPI(DoodPmExtractor())
        registerExtractorAPI(LuluStream())
        registerExtractorAPI(FileMoonIn())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(LuluVdo())
        registerExtractorAPI(LuluPvp())
        registerExtractorAPI(LuluVid())
        registerExtractorAPI(Luludlc())
        registerExtractorAPI(Lulu0())
        registerExtractorAPI(LuluVdoo())
        registerExtractorAPI(VidNest())
        registerExtractorAPI(Lulux08())
        registerExtractorAPI(HgLink())
        registerExtractorAPI(RyderJet())
        registerExtractorAPI(MyCloudZ())
        registerExtractorAPI(VidStack())
        registerExtractorAPI(StbP2P())
        registerExtractorAPI(Playerupnone())
        registerExtractorAPI(Turtleviplay())
        registerExtractorAPI(Turboviplay())
    }
}
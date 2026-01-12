// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class HentailaPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Hentaila())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(PixelDrain())
        registerExtractorAPI(Mp4Upload())
        registerExtractorAPI(Mp4Upload())
        registerExtractorAPI(VidHidePro())
        registerExtractorAPI(VidHidePro1())
        registerExtractorAPI(VidHidePro2())
        registerExtractorAPI(VidHidePro3())
        registerExtractorAPI(VidHidePro4())
        registerExtractorAPI(VidHidePro5())
        registerExtractorAPI(VidHidePro6())
        registerExtractorAPI(VidHidePro7())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Smoothpre())
        registerExtractorAPI(Dhtpre())
        registerExtractorAPI(Peytonepre())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Dintezuvio())
        registerExtractorAPI(Riderjet())
    }
}
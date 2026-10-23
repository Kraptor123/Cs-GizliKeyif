// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.

package com.kraptor
import com.kraptor.ortak.extractor.*

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.extractors.EmturbovidExtractor
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.extractors.Voe

@CloudstreamPlugin
class MangopornProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Mangoporn())

        Filemoon.DOMAINS.forEach { registerExtractorAPI(Filemoon(it)) }
        VidHidePro.DOMAINS.forEach { registerExtractorAPI(VidHidePro(it)) }
        Streamwish.DOMAINS.forEach { registerExtractorAPI(Streamwish(it)) }
        LULUBASE.DOMAINS.forEach { registerExtractorAPI(LULUBASE(it)) }
        Player4Me.DOMAINS.forEach { registerExtractorAPI(Player4Me(it)) }
        DoodStream.DOMAINS.forEach { registerExtractorAPI(DoodStream(it)) }
        StreamTAPE.DOMAINS.forEach { registerExtractorAPI(StreamTAPE(it)) }
        Maxstream.DOMAINS.forEach { registerExtractorAPI(Maxstream(it)) }
        VOE.DOMAINS.forEach { registerExtractorAPI(VOE(it)) }

        registerExtractorAPI(CloudWish())
        registerExtractorAPI(MixDropis())
        registerExtractorAPI(Javggvideo())
        registerExtractorAPI(EmturbovidExtractor())
        registerExtractorAPI(VidNest())
        registerExtractorAPI(VidStack())
        registerExtractorAPI(StbP2P())
        registerExtractorAPI(Playerupnone())
        registerExtractorAPI(Turtleviplay())
        registerExtractorAPI(Turboviplay())
        registerExtractorAPI(Vidguardto())
        registerExtractorAPI(MixDropAG())
        registerExtractorAPI(MixDropMy())
        registerExtractorAPI(Vinovo())
        registerExtractorAPI(Playmate())

        this.openSettings = { ctx: Context ->
            MangoAyarlar.showSettingsDialog(ctx as AppCompatActivity) {
                MainActivity.reloadHomeEvent.invoke(true)
            }
        }
    }
}

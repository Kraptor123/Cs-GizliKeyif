// ! This Extension Made By @kraptor for GizliKeyif

package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class Film1KPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Film1K())
        registerExtractorAPI(TurboVidHlsExtractor())

        // ! Byse / Filemoon altyapisi - base + ayni akisi kullanan tum host'lar
        registerExtractorAPI(ByseExtractor())
        registerExtractorAPI(F16px())
        registerExtractorAPI(ByseSayeveum())
        registerExtractorAPI(ByseTayico())
        registerExtractorAPI(ByseVepoin())
        registerExtractorAPI(ByseZejataos())
        registerExtractorAPI(ByseKoze())
        registerExtractorAPI(ByseSukior())
        registerExtractorAPI(ByseJikuar())
        registerExtractorAPI(ByseFujedu())
        registerExtractorAPI(ByseDikamoum())
        registerExtractorAPI(ByseBuho())
        registerExtractorAPI(ByseWihe())
        registerExtractorAPI(ByseLapuix())
        registerExtractorAPI(ByseQekaho())
        registerExtractorAPI(EmbedPlayByse())
        registerExtractorAPI(FilemoonSx())
        registerExtractorAPI(FilemoonTo())
        registerExtractorAPI(FilemoonIn())
        registerExtractorAPI(FilemoonLink())
        registerExtractorAPI(FilemoonNl())
        registerExtractorAPI(FilemoonWf())
        registerExtractorAPI(FilemoonEu())
        registerExtractorAPI(FilemoonArt())
        registerExtractorAPI(MoonMov())
        registerExtractorAPI(Cinegrab())
        registerExtractorAPI(Ar96())
        registerExtractorAPI(Kerapoxy())
        registerExtractorAPI(Furher())
        registerExtractorAPI(Azayf9w())
        registerExtractorAPI(U6xl9d())
        registerExtractorAPI(Smdfs40r())
        registerExtractorAPI(C1z39())
        registerExtractorAPI(Bf0skv())
        registerExtractorAPI(Z1ekv717())
        registerExtractorAPI(L1afav())
        registerExtractorAPI(I8x222())
        registerExtractorAPI(Mhlloqo())
        registerExtractorAPI(F51rm())
        registerExtractorAPI(Xcoic())
        registerExtractorAPI(BoosterAdx())
        registerExtractorAPI(StreamlyPlayer())
        registerExtractorAPI(StreamlyPlayerO())
        registerExtractorAPI(RupertIsDiving())
        registerExtractorAPI(Sb1254w9())
        registerExtractorAPI(Film1KByse())
    }
}

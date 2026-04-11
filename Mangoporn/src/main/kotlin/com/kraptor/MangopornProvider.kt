package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.extractors.EmturbovidExtractor
import com.lagradost.cloudstream3.extractors.FileMoonSx
import com.lagradost.cloudstream3.extractors.StreamTape

@CloudstreamPlugin
class MangopornProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Mangoporn())
        registerExtractorAPI(EmturbovidExtractor())
        registerExtractorAPI(FileMoonSx())
        registerExtractorAPI(DoodPmExtractor())
        registerExtractorAPI(Vidguardto())
        registerExtractorAPI(MixDropAG())
        registerExtractorAPI(MixDropMy())
        registerExtractorAPI(LuluStream())
        registerExtractorAPI(FileMoon2())
        registerExtractorAPI(FileMoonIn())
        registerExtractorAPI(FileMoonSx())
        registerExtractorAPI(Bysedikamoum())
        registerExtractorAPI(Bysezoexe())
        registerExtractorAPI(Filemoonx08())
        registerExtractorAPI(StreamTape())
        registerExtractorAPI(Player4Me())
        registerExtractorAPI(DoodStream())
        registerExtractorAPI(LuluVdo())
        registerExtractorAPI(LuluPvp())
        registerExtractorAPI(LuluVid())
        registerExtractorAPI(Luludlc())
        registerExtractorAPI(Lulu0())
        registerExtractorAPI(LuluVdoo())
        registerExtractorAPI(Vip4me())
        registerExtractorAPI(VidNest())
        registerExtractorAPI(RPMShare())
        registerExtractorAPI(Playmogo())
        registerExtractorAPI(DoodDoply())



        this.openSettings = { ctx: Context ->
            MangoAyarlar.showSettingsDialog(ctx as AppCompatActivity) {
                MainActivity.reloadHomeEvent.invoke(true)
            }
        }
    }
}
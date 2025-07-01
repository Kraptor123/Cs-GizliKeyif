// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.keyiflerolsun.MixPlayHD
import com.keyiflerolsun.MixTiger
import com.keyiflerolsun.SuperErotikGeldi
import com.lagradost.cloudstream3.extractors.MixDrop

@CloudstreamPlugin
class SuperErotikGeldiPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SuperErotikGeldi())
        registerExtractorAPI(MixPlayHD())
        registerExtractorAPI(MixTiger())
        registerExtractorAPI(MixDrop())
        registerExtractorAPI(PlayerFilmIzle())
    }
}
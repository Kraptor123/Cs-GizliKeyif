// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.MixDrop
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class SuperErotikGeldiPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(SuperErotikGeldi())
        registerExtractorAPI(MixPlayHD())
        registerExtractorAPI(MixTiger())
        registerExtractorAPI(MixDrop())
        registerExtractorAPI(PlayerFilmIzle())
    }
}
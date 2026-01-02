package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class FapixPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Fapix())
    }
}
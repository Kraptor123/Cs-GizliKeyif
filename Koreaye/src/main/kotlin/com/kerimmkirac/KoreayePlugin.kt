package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class KoreayePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Koreaye())
    }
}
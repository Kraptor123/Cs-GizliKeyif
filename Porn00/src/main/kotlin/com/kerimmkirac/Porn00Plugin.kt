package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class Porn00Plugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Porn00())
    }
}
package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class CamWhPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(CamWh())
    }
}
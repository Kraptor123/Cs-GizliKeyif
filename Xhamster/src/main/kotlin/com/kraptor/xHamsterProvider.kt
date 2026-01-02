package com.kraptor

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@Suppress("ClassName")
@CloudstreamPlugin
class xHamsterProvider: BasePlugin() {
    override fun load() {
        registerMainAPI(xHamster())
    }
}
package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class DomlepenPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Domlepen())
    }
}
package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class HdAblaPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(HdAbla())
    }
}
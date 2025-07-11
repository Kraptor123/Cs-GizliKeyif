package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class PornoLavaPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PornoLava())
    }
}
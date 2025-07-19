package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class CamWhPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CamWh())
    }
}
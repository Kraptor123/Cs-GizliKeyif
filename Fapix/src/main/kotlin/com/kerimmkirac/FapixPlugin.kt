package com.kerimmkirac

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class FapixPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Fapix(context.cacheDir))
    }
}

package com.kerimmkirac

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class Porn00Plugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Porn00(context))
    }
}
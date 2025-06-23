package com.kraptor

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@Suppress("ClassName")
@CloudstreamPlugin
class xHamsterProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(xHamster())
    }
}
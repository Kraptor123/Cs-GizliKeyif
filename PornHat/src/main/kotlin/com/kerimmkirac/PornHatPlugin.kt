package com.kerimmkirac

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class PornHatPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PornHat())
    }
}
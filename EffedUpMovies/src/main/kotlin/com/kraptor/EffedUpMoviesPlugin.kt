// ! This Extension Made By @kraptor for GizliKeyif

package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class EffedUpMoviesPlugin: Plugin() {
    override fun load() {
        registerMainAPI(EffedUpMovies())
    }
}
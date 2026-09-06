// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class FapGoatPlugin: Plugin() {
    override fun load() {
        registerMainAPI(FapGoat())
    }
}
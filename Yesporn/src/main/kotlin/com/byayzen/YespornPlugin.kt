// ! This Extension Made By @ByAyzen for GizliKeyif

package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class YespornPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Yesporn(context))
    }
}
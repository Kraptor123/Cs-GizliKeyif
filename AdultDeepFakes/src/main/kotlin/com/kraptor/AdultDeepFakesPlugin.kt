// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AdultDeepFakesPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AdultDeepFakes(context))
    }
}
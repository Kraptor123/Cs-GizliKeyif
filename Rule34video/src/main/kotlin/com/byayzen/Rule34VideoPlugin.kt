// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class Rule34VideoPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Rule34Video())
    }
}
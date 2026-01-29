// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TurkifsahubPlugin: Plugin() {
    override fun load() {
        registerMainAPI(Turkifsahub())
    }
}
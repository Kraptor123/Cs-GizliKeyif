// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.byayzen.PMVHaven
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PMVHavenPlugin: Plugin() {
    override fun load() {
        registerMainAPI(PMVHaven())
    }
}
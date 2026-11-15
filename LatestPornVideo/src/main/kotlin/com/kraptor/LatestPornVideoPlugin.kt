// ! Bu araç @Kraptor123 tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.kraptor

import com.kraptor.ortak.extractor.LULUBASE
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class LatestPornVideoPlugin: Plugin() {
    override fun load() {
        registerMainAPI(LatestPornVideo())
        LULUBASE.DOMAINS.forEach { registerExtractorAPI(LULUBASE(it)) }
    }
}

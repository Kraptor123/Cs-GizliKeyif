// ! Bu araç @ByAyzen tarafından | @Siklikeyif için yazılmıştır.
package com.byayzen

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class KoreanpornmoviePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Koreanpornmovie())
    }
}
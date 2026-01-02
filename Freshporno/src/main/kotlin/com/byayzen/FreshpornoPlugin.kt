// ! Bu araç @ByAyzen tarafından | @Cs-GizliKeyif için yazılmıştır.
package com.byayzen

import com.byayzen.Freshporno
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class FreshpornoPlugin : Plugin() {
    override fun load() {
        registerMainAPI(Freshporno())
    }
}
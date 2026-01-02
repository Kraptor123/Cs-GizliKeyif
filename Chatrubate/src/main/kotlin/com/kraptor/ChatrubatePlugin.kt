package com.kraptor

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class ChatrubatePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Chatrubate())
    }
}
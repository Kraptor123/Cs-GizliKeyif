package com.kerimmkirac

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class Creator(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("service") val service: String,
    @JsonProperty("indexed") val indexed: Long,
    @JsonProperty("updated") val updated: Long,
    @JsonProperty("favorited") val favorited: Int
)

class Coomer : MainAPI() {
    override var mainUrl              = "https://coomer.su"
    override var name                 = "Coomer"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)

    

    override val mainPage = mainPageOf(
        "creators"      to "Creators",
    )

    
    private suspend fun parseCreatorsStream(limit: Int = 100): List<Creator> {
        val apiUrl = "${mainUrl}/api/v1/creators.txt"
        

        try {
            val response = app.get(apiUrl)
            
            
            val inputStream = response.body?.byteStream()

            if (inputStream == null) {
                
                return emptyList()
            }

            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            val creators = mutableListOf<Creator>()
            val mapper = jacksonObjectMapper()

            
            val buffer = CharArray(50000) 
            val charsRead = reader.read(buffer)
            reader.close()

            

            if (charsRead == -1) {
                
                return emptyList()
            }

            val jsonText = String(buffer, 0, charsRead)
            

           
            val startIndex = jsonText.indexOf('[')
            if (startIndex == -1) {
                
                return emptyList()
            }

            

            var currentIndex = startIndex + 1
            var bracketCount = 0
            var inString = false
            var escaped = false
            var objectStart = -1

            for (i in currentIndex until jsonText.length) {
                val char = jsonText[i]

                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = !inString
                    !inString && char == '{' -> {
                        if (objectStart == -1) objectStart = i
                        bracketCount++
                    }
                    !inString && char == '}' -> {
                        bracketCount--
                        if (bracketCount == 0 && objectStart != -1) {
                            
                            try {
                                val objectJson = jsonText.substring(objectStart, i + 1)
                                val creator = mapper.readValue<Creator>(objectJson)
                                creators.add(creator)

                                if (creators.size >= limit) {
                                   
                                    break
                                }

                                objectStart = -1
                            } catch (e: Exception) {
                               
                                objectStart = -1
                            }
                        }
                    }
                }
            }

            
            return creators

        } catch (e: Exception) {
            
            return emptyList()
        }
    }

    
    private suspend fun searchCreatorsStream(query: String, limit: Int = 50): List<Creator> {
        val apiUrl = "${mainUrl}/api/v1/creators.txt"
        

        if (query.isBlank()) {
            
            return emptyList()
        }

        try {
            val response = app.get(apiUrl)
           
            
            val inputStream = response.body?.byteStream()

            if (inputStream == null) {
                
                return emptyList()
            }

            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            val creators = mutableListOf<Creator>()
            val mapper = jacksonObjectMapper()

            
            val buffer = CharArray(200000) 
            val charsRead = reader.read(buffer)
            reader.close()

            

            if (charsRead == -1) {
               
                return emptyList()
            }

            val jsonText = String(buffer, 0, charsRead)
           

            
            val startIndex = jsonText.indexOf('[')
            if (startIndex == -1) {
                
                return emptyList()
            }

           

            var currentIndex = startIndex + 1
            var bracketCount = 0
            var inString = false
            var escaped = false
            var objectStart = -1
            var processedObjects = 0

            for (i in currentIndex until jsonText.length) {
                val char = jsonText[i]

                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = !inString
                    !inString && char == '{' -> {
                        if (objectStart == -1) objectStart = i
                        bracketCount++
                    }
                    !inString && char == '}' -> {
                        bracketCount--
                        if (bracketCount == 0 && objectStart != -1) {
                           
                            try {
                                val objectJson = jsonText.substring(objectStart, i + 1)
                                val creator = mapper.readValue<Creator>(objectJson)
                                processedObjects++
                                
                                
                                val queryLower = query.lowercase()
                                val nameMatch = creator.name.lowercase().contains(queryLower)
                                val idMatch = creator.id.lowercase().contains(queryLower)
                                
                                if (nameMatch || idMatch) {
                                    creators.add(creator)
                                    
                                }

                                if (creators.size >= limit) {
                                    
                                    break
                                }

                                objectStart = -1
                            } catch (e: Exception) {
                                
                                objectStart = -1
                            }
                        }
                    }
                }
            }

            
            
            
            if (creators.isEmpty()) {
                
                return searchCreatorsPartial(query, limit)
            }
            
            return creators

        } catch (e: Exception) {
            
            return emptyList()
        }
    }

    
    private suspend fun searchCreatorsPartial(query: String, limit: Int = 50): List<Creator> {
        
        
        val allCreators = parseCreatorsStream(500) 
        val queryLower = query.lowercase()
        
        val matches = allCreators.filter { creator ->
            val nameMatch = creator.name.lowercase().contains(queryLower)
            val idMatch = creator.id.lowercase().contains(queryLower)
            val serviceMatch = creator.service.lowercase().contains(queryLower)
            
            nameMatch || idMatch || serviceMatch
        }.take(limit)
        
        
        return matches
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        
        
        val creators = parseCreatorsStream(100)
        

        val home = creators.mapNotNull { creator ->
            creator.toSearchResponse()
        }

        
        return newHomePageResponse(request.name, home)
    }

    private fun Creator.toSearchResponse(): SearchResponse? {
        val title = this.name
        val href = "${mainUrl}/api/v1/${this.service}/user/${this.id}/profile"
        val posterUrl = "https://img.coomer.su/icons/${this.service}/${this.id}"

        

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
       
        
        if (query.isBlank()) {
            
            return emptyList()
        }

        val creators = searchCreatorsStream(query, 50)
       

        val searchResponses = creators.mapNotNull { creator ->
            creator.toSearchResponse()
        }

        
        return searchResponses
    }

    override suspend fun load(url: String): LoadResponse? {
        
        
        try {
            
            val profileResponse = app.get(url).text
            

            
            val urlParts = url.split("/")
            val service = urlParts[urlParts.indexOf("v1") + 1]
            val id = urlParts[urlParts.indexOf("user") + 1]
            
            

           
            val mapper = jacksonObjectMapper()
            val profileData = try {
                mapper.readValue<Map<String, Any>>(profileResponse)
            } catch (e: Exception) {
               
                null
            }

            val name = profileData?.get("name")?.toString() ?: id
            val posterUrl = "https://img.coomer.su/icons/${service}/${id}"
            val bannerUrl = "https://img.coomer.su/banners/${service}/${id}"

            

            return newMovieLoadResponse(name, url, TvType.Movie, url) {
                this.posterUrl = bannerUrl
                this.plot = "Creator: $name\nService: $service\nID: $id"
            }
        } catch (e: Exception) {
            
            return null
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
       
       
        return true
    }
}
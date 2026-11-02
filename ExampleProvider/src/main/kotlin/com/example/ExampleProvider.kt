package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder

class ExampleProvider : MainAPI() {

    override var mainUrl = "https://www.aparat.com/"
    override var name = "Aparat"
    override var lang = "fa"
    override val hasMainPage = false
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return emptyList()

        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")

            val response = app.get(
                "${mainUrl}etc/api/videoBySearch/text/$encodedQuery/perpage/20"
            ).text

            val json = JSONObject(response)
            val results = mutableListOf<SearchResponse>()

            fun readData(value: Any?) {
                when (value) {
                    is JSONArray -> {
                        for (i in 0 until value.length()) {
                            readData(value.opt(i))
                        }
                    }

                    is JSONObject -> {
                        val title = value.optString("title")
                        val id = value.optString("uid")
                            .ifBlank { value.optString("hash_id") }

                        val link = value.optString("url")
                            .ifBlank {
                                if (id.isNotBlank())
                                    "${mainUrl}v/$id"
                                else ""
                            }

                        val poster = value.optString("big_poster")
                            .ifBlank {
                                value.optString("small_poster")
                            }
                            .ifBlank {
                                value.optString("thumbnail")
                            }

                        if (title.isNotBlank() && link.isNotBlank()) {
                            results.add(
                                newMovieSearchResponse(
                                    title,
                                    link,
                                    TvType.Movie
                                ) {
                                    posterUrl = poster.ifBlank { null }
                                }
                            )
                        } else {
                            val keys = value.keys()
                            while (keys.hasNext()) {
                                readData(value.opt(keys.next()))
                            }
                        }
                    }
                }
            }

            readData(json)
            results.distinctBy { it.url }

        } catch (e: Exception) {
            emptyList()
        }
    }
}

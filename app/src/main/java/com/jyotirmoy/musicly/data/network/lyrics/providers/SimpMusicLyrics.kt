package com.jyotirmoy.musicly.data.network.lyrics.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class SimpMusicApiResponse(
    val success: Boolean,
    val data: List<LyricsData> = emptyList()
)

@Serializable
data class LyricsData(
    val id: String? = null,
    val plainLyric: String? = null,
    val syncedLyrics: String? = null,
    val richSyncLyrics: String? = null,
    val durationSeconds: Int? = null,
    val artistName: String? = null,
    val trackName: String? = null
)

object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
            }

            expectSuccess = false
        }
    }

    suspend fun getLyrics(
        videoId: String,
        duration: Int = 0,
    ): Result<LyricsData> = runCatching {
        val response = client.get(BASE_URL + videoId)
        
        if (response.status == HttpStatusCode.OK) {
            val apiResponse = response.body<SimpMusicApiResponse>()
            if (apiResponse.success && apiResponse.data.isNotEmpty()) {
                val tracks = apiResponse.data
                
                // Filter tracks that match duration within tolerance (15 seconds)
                val validTracks = if (duration > 0) {
                    tracks.filter { track ->
                        abs((track.durationSeconds ?: 0) - duration) <= 15
                    }
                } else {
                    tracks
                }

                val bestMatch = if (duration > 0 && validTracks.size > 1) {
                    validTracks.minByOrNull { track ->
                        abs((track.durationSeconds ?: 0) - duration)
                    }
                } else {
                    validTracks.firstOrNull() ?: tracks.firstOrNull()
                }
                
                bestMatch ?: throw IllegalStateException("Lyrics not found in data")
            } else {
                throw IllegalStateException("Lyrics unavailable")
            }
        } else {
            throw IllegalStateException("Server error: ${response.status}")
        }
    }
}

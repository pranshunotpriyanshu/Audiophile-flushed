package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

class SimpMusicProvider : LyricsProvider {
    override val name: String = "SimpMusic"

    @Serializable
    private data class SimpMusicApiResponse(
        val type: String? = null,
        val data: List<SimpLyricsData> = emptyList(),
    ) {
        val success: Boolean get() = type == "success"
    }

    @Serializable
    private data class SimpLyricsData(
        val id: String? = null,
        val videoId: String? = null,
        @SerialName("songTitle") val title: String? = null,
        @SerialName("artistName") val artist: String? = null,
        @SerialName("albumName") val album: String? = null,
        @SerialName("durationSeconds") val duration: Int? = null,
        val syncedLyrics: String? = null,
        @SerialName("plainLyric") val plainLyrics: String? = null,
    )

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(this@SimpMusicProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            defaultRequest {
                url("https://api-lyrics.simpmusic.org/v1/")
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }
            expectSuccess = false
        }
    }

    override suspend fun fetch(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long?,
        videoId: String?,
    ): AudiophileLyrics? {
        val cleanVideoId = videoId?.trim()
        if (cleanVideoId.isNullOrBlank()) return null

        val durationSeconds = if (durationMs != null && durationMs > 0) (durationMs / 1000).toInt() else 0

        return try {
            val response = client.get("https://api-lyrics.simpmusic.org/v1/$cleanVideoId")
            if (response.status != HttpStatusCode.OK) return null

            val apiResponse = response.body<SimpMusicApiResponse>()
            if (!apiResponse.success || apiResponse.data.isEmpty()) return null

            val bestMatch: SimpLyricsData = if (durationSeconds > 0 && apiResponse.data.size > 1) {
                apiResponse.data.minByOrNull { abs((it.duration ?: 0) - durationSeconds) } ?: apiResponse.data.first()
            } else {
                apiResponse.data.first()
            }

            val syncedLyrics = bestMatch.syncedLyrics?.takeIf { it.isNotBlank() }
            val plainLyrics = bestMatch.plainLyrics?.takeIf { it.isNotBlank() }
            val lyrics = syncedLyrics ?: plainLyrics ?: return null

            AudiophileLyrics(
                provider = name,
                text = lyrics,
                isWordSynced = syncedLyrics != null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

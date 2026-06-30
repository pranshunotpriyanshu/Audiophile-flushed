package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class UnisonProvider : LyricsProvider {
    override val name: String = "Unison"

    @Serializable
    private data class UnisonEntry(
        val id: Long = 0,
        @SerialName("videoId") val videoId: String? = null,
        val song: String = "",
        val artist: String = "",
        val lyrics: String = "",
        val format: String = "",
        val syncType: String = "",
    )

    @Serializable
    private data class UnisonResponse(
        val success: Boolean = false,
        val data: UnisonEntry? = null,
    )

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(this@UnisonProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 20000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 20000
            }
            defaultRequest { url("https://unison.boidu.dev/") }
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
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return null
        val durationSeconds = if (durationMs != null && durationMs > 0) (durationMs / 1000).toInt() else -1

        return try {
            if (!videoId.isNullOrBlank()) {
                val byId = fetchByVideoId(videoId)
                if (byId != null) return byId
            }
            fetchByMetadata(cleanTitle, cleanArtist, album?.trim(), durationSeconds)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchByVideoId(videoId: String): AudiophileLyrics? {
        return try {
            val response = client.get("lyrics") { parameter("v", videoId) }
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            val parsed = runCatching { json.decodeFromString<UnisonResponse>(body) }.getOrNull()
            val entry = parsed?.takeIf { it.success }?.data?.takeIf { it.lyrics.isNotBlank() } ?: return null
            AudiophileLyrics(provider = name, text = entry.lyrics, isWordSynced = entry.format.contains("lrc", ignoreCase = true))
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private suspend fun fetchByMetadata(title: String, artist: String, album: String?, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val response = client.get("lyrics") {
                parameter("song", title)
                parameter("artist", artist)
                if (!album.isNullOrBlank()) parameter("album", album)
                if (durationSeconds > 0) parameter("duration", durationSeconds)
            }
            if (!response.status.isSuccess()) return null
            val body = response.bodyAsText()
            val parsed = runCatching { json.decodeFromString<UnisonResponse>(body) }.getOrNull()
            val entry = parsed?.takeIf { it.success }?.data?.takeIf { it.lyrics.isNotBlank() } ?: return null
            AudiophileLyrics(provider = name, text = entry.lyrics, isWordSynced = entry.format.contains("lrc", ignoreCase = true))
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }
}

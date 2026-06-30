package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.abs

class PaxsenixProvider : LyricsProvider {
    override val name: String = "Paxsenix"

    private val BASE_URL = "https://lyrics.paxsenix.org/"

    @Serializable
    private data class PaxsenixSearchItem(
        val id: String? = null,
        @SerialName("trackId") val trackId: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val name: String? = null,
        val duration: JsonElement? = null,
    ) {
        val realId: String get() = id ?: trackId ?: ""
        val durationMs: Long
            get() {
                val primitive = try { duration?.jsonPrimitive } catch (_: Exception) { null } ?: return 0
                return primitive.longOrNull ?: run {
                    val parts = primitive.content.trim().split(":")
                    if (parts.size >= 2) {
                        val seconds = parts.last().toLongOrNull() ?: 0
                        val minutes = parts[parts.size - 2].toLongOrNull() ?: 0
                        (minutes * 60 + seconds) * 1000
                    } else 0
                }
            }
    }

    @Serializable
    private data class PaxsenixNeteaseSearchResponse(val result: PaxsenixNeteaseSearchResult? = null)

    @Serializable
    private data class PaxsenixNeteaseSearchResult(val songs: List<PaxsenixNeteaseSong> = emptyList())

    @Serializable
    private data class PaxsenixNeteaseSong(val id: Long = 0, val name: String? = null, val duration: Int = 0)

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(this@PaxsenixProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.UserAgent, "Flamingo/1.0")
                header(HttpHeaders.Accept, "application/json, text/plain, */*")
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
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank() || cleanArtist.isBlank()) return null
        val durationSeconds = if (durationMs != null && durationMs > 0) (durationMs / 1000).toInt() else -1

        val amResult = try { fetchAppleMusicLyrics(cleanTitle, cleanArtist, durationSeconds) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        if (amResult != null) return amResult

        val neResult = try { fetchNeteaseLyrics(cleanTitle, cleanArtist, durationSeconds) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        if (neResult != null) return neResult

        val spResult = try { fetchSpotifyLyrics(cleanTitle, cleanArtist, durationSeconds) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        if (spResult != null) return spResult

        val ytResult = try { fetchYouTubeLyrics(cleanTitle, cleanArtist, durationSeconds) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        if (ytResult != null) return ytResult

        val mxResult = try { fetchMusixmatchLyrics(cleanTitle, cleanArtist, durationSeconds) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        if (mxResult != null) return mxResult

        return null
    }

    private fun resolveDurationMs(duration: Int): Long = when {
        duration <= 0 -> 0L
        duration > 360000 -> duration.toLong()
        else -> duration * 1000L
    }

    private suspend fun fetchAppleMusicLyrics(title: String, artist: String, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val query = "$title $artist"
            val lyricsResponse = client.get("apple-music/lyrics") { parameter("q", query) }
            if (lyricsResponse.status != HttpStatusCode.OK) return null
            val body = lyricsResponse.body<String>().trim()

            if (body.startsWith("<tt") || body.startsWith("<?xml")) {
                return AudiophileLyrics(provider = "$name (Apple Music)", text = body, isWordSynced = true)
            }

            val data = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
            val obj = data as? kotlinx.serialization.json.JsonObject ?: return null
            val lyrics = obj["lyrics"]?.jsonPrimitive?.content
                ?: obj["lrc"]?.jsonPrimitive?.content
                ?: obj["content"]?.jsonPrimitive?.content
            if (!lyrics.isNullOrBlank()) {
                return AudiophileLyrics(provider = "$name (Apple Music)", text = lyrics, isWordSynced = lyrics.contains("["))
            }
            null
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private suspend fun fetchNeteaseLyrics(title: String, artist: String, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val query = "$title $artist"
            val searchResponse = client.get("netease/search") { parameter("q", query) }
            if (searchResponse.status != HttpStatusCode.OK) return null
            val searchResult = searchResponse.body<PaxsenixNeteaseSearchResponse>()
            val songs = searchResult.result?.songs ?: return null
            if (songs.isEmpty()) return null

            val durationMs = resolveDurationMs(durationSeconds)
            val bestMatch: PaxsenixNeteaseSong? = if (durationMs > 0) {
                songs.minByOrNull { abs(it.duration.toLong() - durationMs) }
            } else songs.firstOrNull()

            if (bestMatch == null) return null
            val diff = abs(bestMatch.duration.toLong() - durationMs)
            if (durationMs > 0 && diff >= 10000) return null

            val lyricsResponse = client.get("netease/lyrics") {
                parameter("id", bestMatch.id)
                parameter("word", "true")
            }
            if (lyricsResponse.status != HttpStatusCode.OK) return null
            val body = lyricsResponse.body<kotlinx.serialization.json.JsonObject>()

            val klyric = body["klyric"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
            if (!klyric.isNullOrBlank()) {
                return AudiophileLyrics(provider = "$name (NetEase)", text = klyric, isWordSynced = true)
            }
            val lrc = body["lrc"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content
            if (!lrc.isNullOrBlank()) {
                return AudiophileLyrics(provider = "$name (NetEase)", text = lrc, isWordSynced = lrc.contains("["))
            }
            null
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private suspend fun fetchSpotifyLyrics(title: String, artist: String, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val query = "$title $artist"
            val searchResponse = client.get("spotify/search") { parameter("q", query) }
            if (searchResponse.status != HttpStatusCode.OK) return null
            val items = searchResponse.body<List<PaxsenixSearchItem>>()
            if (items.isEmpty()) return null

            val durationMs = resolveDurationMs(durationSeconds)
            val bestMatch: PaxsenixSearchItem? = if (durationMs > 0) {
                items.minByOrNull { abs(it.durationMs - durationMs) }
            } else items.firstOrNull()

            if (bestMatch == null) return null
            val diff = abs(bestMatch.durationMs - durationMs)
            if (durationMs > 0 && diff >= 10000) return null

            val lyricsResponse = client.get("spotify/lyrics") { parameter("id", bestMatch.realId) }
            if (lyricsResponse.status != HttpStatusCode.OK) return null
            val data = lyricsResponse.body<String>().trim()
            val parsed = cleanJsonLyrics(data)
            if (!parsed.isNullOrBlank()) {
                AudiophileLyrics(provider = "$name (Spotify)", text = parsed, isWordSynced = parsed.contains("["))
            } else null
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private suspend fun fetchYouTubeLyrics(title: String, artist: String, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val query = "$title $artist"
            val searchResponse = client.get("youtube/search") { parameter("q", query) }
            if (searchResponse.status != HttpStatusCode.OK) return null
            val items = searchResponse.body<List<PaxsenixSearchItem>>()
            if (items.isEmpty()) return null

            val durationMs = resolveDurationMs(durationSeconds)
            val bestMatch: PaxsenixSearchItem? = if (durationMs > 0) {
                items.minByOrNull { abs(it.durationMs - durationMs) }
            } else items.firstOrNull()

            if (bestMatch == null) return null
            val diff = abs(bestMatch.durationMs - durationMs)
            if (durationMs > 0 && diff >= 10000) return null

            val lyricsResponse = client.get("youtube/lyrics") { parameter("id", bestMatch.realId) }
            if (lyricsResponse.status != HttpStatusCode.OK) return null
            val data = lyricsResponse.body<String>().trim()
            val parsed = cleanJsonLyrics(data)
            if (!parsed.isNullOrBlank()) {
                AudiophileLyrics(provider = "$name (YouTube)", text = parsed, isWordSynced = parsed.contains("["))
            } else null
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private suspend fun fetchMusixmatchLyrics(title: String, artist: String, durationSeconds: Int): AudiophileLyrics? {
        return try {
            val query = "$title $artist"

            val wordResponse = client.get("musixmatch/lyrics") {
                parameter("q", query); parameter("t", title); parameter("a", artist)
                parameter("d", durationSeconds.toString()); parameter("type", "word")
            }
            if (wordResponse.status == HttpStatusCode.OK) {
                val data = cleanJsonLyrics(wordResponse.body<String>())
                if (!data.isNullOrBlank()) {
                    return AudiophileLyrics(provider = "$name (Musixmatch)", text = data, isWordSynced = true)
                }
            }

            val defaultResponse = client.get("musixmatch/lyrics") {
                parameter("q", query); parameter("t", title); parameter("a", artist)
                parameter("d", durationSeconds.toString())
            }
            if (defaultResponse.status == HttpStatusCode.OK) {
                val data = cleanJsonLyrics(defaultResponse.body<String>())
                if (!data.isNullOrBlank()) {
                    return AudiophileLyrics(provider = "$name (Musixmatch)", text = data, isWordSynced = data.contains("["))
                }
            }
            null
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    }

    private fun cleanJsonLyrics(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val payload = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return trimmed
        return extractLyrics(payload)
    }

    private fun extractLyrics(element: kotlinx.serialization.json.JsonElement): String? = when (element) {
        is kotlinx.serialization.json.JsonNull -> null
        is kotlinx.serialization.json.JsonPrimitive -> {
            if (!element.isString) null
            else {
                val value = element.content.trim()
                if (value.isEmpty()) null else {
                    val nested = runCatching { json.parseToJsonElement(value) }.getOrNull()
                    if (nested != null && nested !is kotlinx.serialization.json.JsonPrimitive) extractLyrics(nested) else value
                }
            }
        }
        is kotlinx.serialization.json.JsonArray -> element.mapNotNull(::extractLyrics).joinToString("\n").trim().takeIf { it.isNotEmpty() }
        is kotlinx.serialization.json.JsonObject -> {
            if (element["error"] != null && element["error"] !is kotlinx.serialization.json.JsonNull) null
            else {
                listOf("lyrics", "lrc", "content", "text", "plainLyrics", "syncedLyrics")
                    .asSequence()
                    .mapNotNull { key -> element[key]?.let(::extractLyrics) }
                    .firstOrNull()
                    ?: element["words"]?.let { words ->
                        when (words) {
                            is kotlinx.serialization.json.JsonArray -> words.mapNotNull(::extractLyrics).joinToString(" ").trim().takeIf { it.isNotEmpty() }
                            else -> extractLyrics(words)
                        }
                    }
            }
        }
    }
}

package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.client.call.body
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.math.abs

class KuGouProvider : LyricsProvider {
    override val name: String = "KuGou"

    @Serializable
    private data class KuGouSearchSongResponse(
        val status: Int = 0,
        val data: SongData = SongData(),
    ) {
        @Serializable
        data class SongData(val info: List<SongInfo> = emptyList())
        @Serializable
        data class SongInfo(val duration: Int = 0, val hash: String = "")
    }

    @Serializable
    private data class KuGouSearchLyricsResponse(
        val status: Int = 0,
        val candidates: List<Candidate> = emptyList(),
    ) {
        @Serializable
        data class Candidate(
            val id: Long = 0,
            @SerialName("product_from") val productFrom: String = "",
            val duration: Long = 0,
            val accesskey: String = "",
        )
    }

    @Serializable
    private data class KuGouDownloadLyricsResponse(val content: String = "")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(this@KuGouProvider.json)
                json(this@KuGouProvider.json, ContentType.Text.Html)
                json(this@KuGouProvider.json, ContentType.Text.Plain)
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
        }
    }

    private fun normalizeTitle(title: String) = title
        .replace("\\(.*\\)".toRegex(), "")
        .replace("（.*）".toRegex(), "")
        .replace("「.*」".toRegex(), "")
        .replace("『.*』".toRegex(), "")
        .replace("<.*>".toRegex(), "")
        .replace("《.*》".toRegex(), "")
        .replace("〈.*〉".toRegex(), "")
        .replace("＜.*＞".toRegex(), "")

    private fun normalizeArtist(artist: String) = artist
        .replace(", ", "、")
        .replace(" & ", "、")
        .replace(".", "")
        .replace("和", "、")
        .replace("\\(.*\\)".toRegex(), "")
        .replace("（.*）".toRegex(), "")

    private fun generateKeyword(title: String, artist: String): String {
        return "${normalizeTitle(title)} - ${normalizeArtist(artist)}"
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

        val durationMsSafe = durationMs ?: -1
        val durationSeconds = if (durationMsSafe > 0) (durationMsSafe / 1000).toInt() else -1

        return try {
            val keyword = generateKeyword(cleanTitle, cleanArtist)
            val candidate = getLyricsCandidate(keyword, durationSeconds)
            candidate?.let {
                val decoded = String(Base64.getDecoder().decode(it.content), Charsets.UTF_8)
                if (decoded.isNotBlank()) {
                    AudiophileLyrics(provider = name, text = decoded.normalizeLrc(), isWordSynced = decoded.contains("["))
                } else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getLyricsCandidate(keyword: String, duration: Int): KuGouDownloadLyricsResponse? {
        // Step 1: Search songs by keyword
        val searchSongResponse = try {
            client.get("https://mobileservice.kugou.com/api/v3/search/song") {
                parameter("version", 9108)
                parameter("plat", 0)
                parameter("pagesize", 8)
                parameter("showtype", 0)
                url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
            }.body<KuGouSearchSongResponse>()
        } catch (_: Exception) { null }

        // Step 2: For each matching song, search lyrics by hash
        if (searchSongResponse != null) {
            for (song in searchSongResponse.data.info) {
                if (duration == -1 || abs(song.duration - duration) <= DURATION_TOLERANCE) {
                    try {
                        val lyricsSearch = client.get("https://lyrics.kugou.com/search") {
                            parameter("ver", 1)
                            parameter("man", "yes")
                            parameter("client", "pc")
                            parameter("hash", song.hash)
                        }.body<KuGouSearchLyricsResponse>()
                        val candidate = lyricsSearch.candidates.firstOrNull()
                        if (candidate != null) {
                            return downloadLyrics(candidate.id, candidate.accesskey)
                        }
                    } catch (_: Exception) { }
                }
            }
        }

        // Step 3: Fallback - search lyrics by keyword directly
        return try {
            val lyricsSearch = client.get("https://lyrics.kugou.com/search") {
                parameter("ver", 1)
                parameter("man", "yes")
                parameter("client", "pc")
                if (duration != -1) parameter("duration", duration * 1000)
                url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
            }.body<KuGouSearchLyricsResponse>()
            val candidate = lyricsSearch.candidates.firstOrNull() ?: return null
            downloadLyrics(candidate.id, candidate.accesskey)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun downloadLyrics(id: Long, accessKey: String): KuGouDownloadLyricsResponse? {
        return try {
            client.get("https://lyrics.kugou.com/download") {
                parameter("fmt", "lrc")
                parameter("charset", "utf8")
                parameter("client", "pc")
                parameter("ver", 1)
                parameter("id", id)
                parameter("accesskey", accessKey)
            }.body<KuGouDownloadLyricsResponse>()
        } catch (_: Exception) {
            null
        }
    }

    private val ACCEPTED_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\].*".toRegex()
    private val BANNED_REGEX = ".+].+[:：].+".toRegex()

    private fun String.normalizeLrc(): String = replace("&apos;", "'")
        .lines()
        .filter { it.matches(ACCEPTED_REGEX) }
        .let { lines ->
            var headCutLine = 0
            for (i in minOf(30, lines.lastIndex) downTo 0) {
                if (lines[i].matches(BANNED_REGEX)) { headCutLine = i + 1; break }
            }
            val filtered = lines.drop(headCutLine)
            var tailCutLine = 0
            for (i in minOf(lines.size - 30, lines.lastIndex) downTo 0) {
                if (lines[lines.lastIndex - i].matches(BANNED_REGEX)) { tailCutLine = i + 1; break }
            }
            filtered.dropLast(tailCutLine).joinToString("\n")
        }

    companion object {
        private const val DURATION_TOLERANCE = 8
    }
}

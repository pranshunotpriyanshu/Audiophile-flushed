package com.pryvn.audiophile.code.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.call.body
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

class LrcLibProvider : LyricsProvider {
    override val name: String = "LrcLib"

    @Serializable
    private data class LrcLibTrack(
        val id: Int = 0,
        val trackName: String = "",
        val artistName: String = "",
        val duration: Double = 0.0,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    )

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(this@LrcLibProvider.json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            defaultRequest { url("https://lrclib.net") }
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
            val tracks = client.get("/api/search") {
                parameter("track_name", cleanTitle)
                parameter("artist_name", cleanArtist)
                if (!album.isNullOrBlank()) parameter("album_name", album.trim())
            }.body<List<LrcLibTrack>>()
                .filter { t -> !t.syncedLyrics.isNullOrBlank() || !t.plainLyrics.isNullOrBlank() }

            if (tracks.isEmpty()) return null

            val bestMatch: LrcLibTrack? = if (durationSeconds == -1) {
                findBestMatch(tracks, cleanTitle, cleanArtist)
            } else {
                val byDuration = tracks.minByOrNull { abs(it.duration.toInt() - durationSeconds) }
                    ?.takeIf { abs(it.duration.toInt() - durationSeconds) <= 2 }
                byDuration ?: findBestMatch(tracks, cleanTitle, cleanArtist)
            } ?: return null

            val lyrics = bestMatch!!.syncedLyrics?.takeIf { it.isNotBlank() }
                ?: bestMatch.plainLyrics?.takeIf { it.isNotBlank() }
                ?: return null

            AudiophileLyrics(
                provider = name,
                text = lyrics,
                isWordSynced = !bestMatch.syncedLyrics.isNullOrBlank()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun findBestMatch(tracks: List<LrcLibTrack>, trackName: String, artistName: String): LrcLibTrack? {
        val normalizedTrack = trackName.trim().lowercase()
        val normalizedArtist = artistName.trim().lowercase()
        return tracks.maxByOrNull { track ->
            var score = 0.0
            score += calculateSimilarity(normalizedTrack, track.trackName.trim().lowercase())
            score += calculateSimilarity(normalizedArtist, track.artistName.trim().lowercase())
            if (track.syncedLyrics != null) score += 0.1
            score / 2.0
        }?.takeIf { track ->
            val trackSim = calculateSimilarity(normalizedTrack, track.trackName.trim().lowercase())
            val artistSim = calculateSimilarity(normalizedArtist, track.artistName.trim().lowercase())
            (trackSim + artistSim) / 2.0 > 0.6
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        if (s1.contains(s2) || s2.contains(s1)) return 0.8
        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0 - (distance.toDouble() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + cost)
            }
        }
        return matrix[len1][len2]
    }
}

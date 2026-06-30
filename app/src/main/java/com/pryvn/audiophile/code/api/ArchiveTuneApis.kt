package com.pryvn.audiophile.code.api

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pryvn.audiophile.code.api.lyrics.AudiophileLyrics
import com.pryvn.audiophile.code.api.lyrics.AudiophileSyncedLine
import com.pryvn.audiophile.code.api.lyrics.AudiophileTranslation
import com.pryvn.audiophile.code.api.lyrics.LyricsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AudiophileOnlineTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
    val source: String
)

fun parseSyncedLyrics(lrcText: String): List<AudiophileSyncedLine> {
    val regex = Regex("""\[(\d{2}):(\d{2}(?:\.\d{2,3})?)\](.*)""")
    return lrcText.lines().mapNotNull { line ->
        regex.find(line)?.let { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val secs = match.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
            val text = match.groupValues[3].trim()
            if (text.isBlank()) return@mapNotNull null
            AudiophileSyncedLine(
                timestamp = (minutes * 60_000 + (secs * 1000).toLong()),
                text = text
            )
        }
    }
}

object ArchiveTuneApis {
    private val gson = Gson()

    suspend fun searchMusic(query: String): Result<List<AudiophileOnlineTrack>> =
        runCatching {
            val body = Http.postJson(
                url = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false",
                payload = youtubeSearchPayload(query),
                headers = youtubeHeaders()
            )
            parseYouTubeSearch(body)
        }

    suspend fun fetchLyrics(
        title: String?,
        artist: String?,
        album: String?,
        durationMs: Long,
        videoId: String? = null
    ): AudiophileLyrics? {
        val cleanTitle = title?.takeIf { it.isNotBlank() } ?: return null
        val cleanArtist = artist?.takeIf { it.isNotBlank() } ?: ""
        val durationSeconds = if (durationMs > 0) durationMs else null
        return LyricsManager.fetchLyrics(
            title = cleanTitle,
            artist = cleanArtist,
            album = album?.takeIf { it.isNotBlank() },
            durationMs = durationSeconds,
            videoId = videoId,
        )
    }

    suspend fun fetchTranslation(
        lyrics: String,
        targetLang: String = "en",
        sourceLang: String? = null
    ): Result<String> = LyricsManager.fetchTranslation(lyrics, targetLang, sourceLang ?: "auto")

    private fun youtubeSearchPayload(query: String): String =
        """
        {
          "context": {
            "client": {
              "clientName": "WEB_REMIX",
              "clientVersion": "1.20240624.01.00",
              "hl": "en",
              "gl": "US"
            }
          },
          "query": ${gson.toJson(query)},
          "params": "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"
        }
        """.trimIndent()

    private fun youtubeHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "Origin" to "https://music.youtube.com",
        "Referer" to "https://music.youtube.com/search"
    )

    private fun parseYouTubeSearch(body: String): List<AudiophileOnlineTrack> {
        val root = gson.fromJson(body, JsonObject::class.java)
        val text = root.toString()
        val videoIdRegex = Regex("\"videoId\":\"([^\"]+)\"")
        val titleRegex = Regex("\"title\"\\s*:\\s*\\{\"runs\"\\s*:\\s*\\[\\{\"text\":\"([^\"]+)\"")
        val ids = videoIdRegex.findAll(text).map { it.groupValues[1] }.distinct().take(20).toList()
        val titles = titleRegex.findAll(text).map { it.groupValues[1] }.toList()
        return ids.mapIndexed { index, id ->
            AudiophileOnlineTrack(
                id = id,
                title = titles.getOrNull(index) ?: id,
                artist = null,
                album = null,
                durationSeconds = null,
                thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                source = "YouTube Music"
            )
        }
    }

}

private object Http {
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        request(url, "GET")
    }

    suspend fun postJson(url: String, payload: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            request(url, "POST", payload, headers)
        }

    private fun request(
        url: String,
        method: String,
        payload: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("User-Agent", "Audiophile/1.0")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (payload != null) {
                doOutput = true
                outputStream.use { it.write(payload.toByteArray()) }
            }
        }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }
}

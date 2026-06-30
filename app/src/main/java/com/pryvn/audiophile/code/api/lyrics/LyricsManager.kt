package com.pryvn.audiophile.code.api.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object LyricsManager {
    private val providers: List<LyricsProvider> = listOf(
        BetterLyricsProvider(),
        LrcLibProvider(),
        KuGouProvider(),
        UnisonProvider(),
        YouLyPlusProvider(),
        SimpMusicProvider(),
        PaxsenixProvider(),
    )

    private val translationProvider = TranslationProvider()

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null,
        videoId: String? = null,
    ): AudiophileLyrics? {
        return withContext(Dispatchers.IO) {
            var bestResult: AudiophileLyrics? = null
            var bestIsWordSynced = false

            for (provider in providers) {
                try {
                    val result = provider.fetch(title, artist, album, durationMs, videoId)
                    if (result != null && result.text.isNotBlank() && !result.text.contains("No lyrics found")) {
                        // Prefer word-synced lyrics
                        val isWordSynced = result.isWordSynced
                        if (isWordSynced && !bestIsWordSynced) {
                            // Found a word-synced result when we didn't have one before
                            bestResult = result
                            bestIsWordSynced = true
                        } else if (!bestIsWordSynced) {
                            // Either both are word-synced or both aren't, take the first one
                            // or we haven't found any result yet
                            if (bestResult == null) {
                                bestResult = result
                                bestIsWordSynced = isWordSynced
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            bestResult
        }
    }

    suspend fun fetchParallel(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null,
        videoId: String? = null,
    ): AudiophileLyrics? = coroutineScope {
        val deferreds = providers.map { provider ->
            async {
                try {
                    provider.fetch(title, artist, album, durationMs, videoId)
                } catch (_: Exception) { null }
            }
        }
        deferreds.awaitAll().firstOrNull { result ->
            result != null && result.text.isNotBlank() && !result.text.contains("No lyrics found")
        }
    }

    suspend fun fetchTranslation(
        lyrics: String,
        targetLang: String = "zh",
        sourceLang: String = "auto",
    ): Result<String> = translationProvider.fetchTranslation(lyrics, targetLang, sourceLang)
}

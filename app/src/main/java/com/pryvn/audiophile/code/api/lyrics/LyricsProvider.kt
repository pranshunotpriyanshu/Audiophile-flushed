package com.pryvn.audiophile.code.api.lyrics

interface LyricsProvider {
    val name: String
    suspend fun fetch(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null,
        videoId: String? = null,
    ): AudiophileLyrics?
}

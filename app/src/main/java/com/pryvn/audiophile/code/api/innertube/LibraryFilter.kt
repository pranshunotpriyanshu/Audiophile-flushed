package com.pryvn.audiophile.code.api.innertube

data class LibraryFilter(val value: String) {
    companion object {
        val LIBRARY = LibraryFilter("FEmusic_library_landing")
        val LIKED = LibraryFilter("FEmusic_liked_playlists")
        val HISTORY = LibraryFilter("FEmusic_history")
        val DOWNLOADS = LibraryFilter("FEmusic_downloads")
    }
}

package com.pryvn.audiophile.code.api.innertube

data class SearchFilter(val value: String) {
    companion object {
        val SONGS = SearchFilter("EgWKAQIIAWoKEAMQBBAJEAoQBQ==")
        val VIDEOS = SearchFilter("EgWKAQIQAWoKEAMQBBAJEAoQBQ==")
        val ALBUMS = SearchFilter("EgWKAQIYAWoKEAMQBBAJEAoQBQ==")
        val ARTISTS = SearchFilter("EgWKAQIgAWoKEAMQBBAJEAoQBQ==")
        val PLAYLISTS = SearchFilter("EgWKAQIsAWoKEAMQBBAJEAoQBQ==")
        val ALL = SearchFilter("")
    }
}

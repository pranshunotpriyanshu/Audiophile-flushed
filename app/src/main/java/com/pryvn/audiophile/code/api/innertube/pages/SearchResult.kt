package com.pryvn.audiophile.code.api.innertube.pages

import kotlinx.serialization.json.*

data class SearchPage(
    val title: String?,
    val videoId: String?,
    val artists: List<String>,
    val artistIds: List<String?>,
    val album: String?,
    val albumId: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val playlistId: String?,
) {
    companion object {
        fun toYTItem(renderer: JsonObject): SearchPage? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.content
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.content
                ?: return null

            val playlistId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.content

            val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray

            val artists = mutableListOf<String>()
            val artistIds = mutableListOf<String?>()
            var album: String? = null
            var albumId: String? = null

            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val navEp = run.jsonObject["navigationEndpoint"]?.jsonObject
                val browseId = navEp?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId?.startsWith("UC") == true) {
                    artists.add(text)
                    artistIds.add(browseId)
                } else if (browseId?.startsWith("MPRE") == true) {
                    album = text
                    albumId = browseId
                }
            }

            val thumbnailUrl = renderer["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            var durationSeconds: Int? = null
            renderer["fixedColumns"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFixedColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?.let { text ->
                    val parts = text.split(":")
                    durationSeconds = when (parts.size) {
                        2 -> parts[0].toIntOrNull()?.let { m -> parts[1].toIntOrNull()?.let { s -> m * 60 + s } }
                        3 -> parts[0].toIntOrNull()?.let { h -> parts[1].toIntOrNull()?.let { m -> parts[2].toIntOrNull()?.let { s -> h * 3600 + m * 60 + s } } }
                        else -> null
                    }
                }

            return SearchPage(
                title = title,
                videoId = videoId,
                artists = artists,
                artistIds = artistIds,
                album = album,
                albumId = albumId,
                thumbnailUrl = thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                durationSeconds = durationSeconds,
                playlistId = playlistId,
            )
        }
    }
}

data class SearchResult(
    val items: List<SearchPage>,
    val continuation: String? = null,
)

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchPage>,
)

data class SearchSuggestionPage(
    val videoId: String?,
    val title: String?,
    val artists: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: JsonObject): SearchSuggestionPage? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null
            val videoId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.content
            val thumbnailUrl = renderer["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull
            val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
            val artists = subtitleRuns?.mapNotNull {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            } ?: emptyList()
            return SearchSuggestionPage(
                videoId = videoId,
                title = title,
                artists = artists,
                thumbnailUrl = thumbnailUrl,
            )
        }
    }
}

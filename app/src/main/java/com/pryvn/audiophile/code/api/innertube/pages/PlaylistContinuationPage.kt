package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.SongItem
import kotlinx.serialization.json.*

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject, playlistId: String? = null): PlaylistContinuationPage {
            val contContents = json["continuationContents"]?.jsonObject
                ?.get("musicPlaylistShelfContinuation")?.jsonObject
                ?: json["continuationContents"]?.jsonObject
                    ?.get("sectionListContinuation")?.jsonObject
                    ?.get("contents")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("musicPlaylistShelfRenderer")?.jsonObject
                ?: return PlaylistContinuationPage(emptyList(), null)

            val songs = contContents["contents"]?.jsonArray
                ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                ?.mapNotNull { parseSongItem(it) }
                .orEmpty()

            val continuation = contContents["continuations"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull

            return PlaylistContinuationPage(songs, continuation)
        }

        private fun parseSongItem(renderer: JsonObject): SongItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null

            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray

            val artists = mutableListOf<com.pryvn.audiophile.code.api.innertube.models.YTItem.Artist>()
            var album: com.pryvn.audiophile.code.api.innertube.models.YTItem.Album? = null

            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val navEp = run.jsonObject["navigationEndpoint"]?.jsonObject
                val browseId = navEp?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId?.startsWith("UC") == true) {
                    artists.add(com.pryvn.audiophile.code.api.innertube.models.YTItem.Artist(name = text, id = browseId))
                } else if (browseId?.startsWith("MPRE") == true) {
                    album = com.pryvn.audiophile.code.api.innertube.models.YTItem.Album(name = text, id = browseId)
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

            val playlistId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            return SongItem(
                id = videoId,
                title = title,
                artists = artists,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                playlistId = playlistId,
            )
        }
    }
}

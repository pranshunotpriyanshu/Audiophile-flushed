package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject): PlaylistPage? {
            val headerRenderer = json["header"]?.jsonObject
            val header = headerRenderer?.get("musicResponsiveHeaderRenderer")?.jsonObject
                ?: headerRenderer?.get("musicEditablePlaylistDetailHeaderRenderer")?.jsonObject
                ?: return null

            val playlistId = json["header"]?.jsonObject
                ?.get("musicResponsiveHeaderRenderer")?.jsonObject
                ?.get("playlistItemData")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull
                ?: json["contents"]?.jsonObject
                    ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                    ?.get("tabs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("tabRenderer")?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject
                    ?.get("contents")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("musicPlaylistShelfRenderer")?.jsonObject
                    ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            val title = header["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

            val authorRuns = header["secondSubtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val authorName = authorRuns?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            val authorId = authorRuns?.firstOrNull()?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val author = if (authorName != null) {
                YTItem.Artist(name = authorName, id = authorId)
            } else null

            val description = header["description"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.takeIf { it.isNotBlank() }

            val songCountText = header["secondSubtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.drop(1)
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

            val thumbnail = header["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val isEditable = header["editablePlaylistHeaderRenderer"] != null ||
                json["header"]?.jsonObject?.get("musicEditablePlaylistDetailHeaderRenderer") != null

            val playEndpoint = header["playButton"]?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject?.let {
                    WatchEndpoint(
                        playlistId = it["playlistId"]?.jsonPrimitive?.contentOrNull,
                    )
                }

            val shuffleEndpoint = header["shuffleButton"]?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject?.let {
                    WatchEndpoint(
                        playlistId = it["playlistId"]?.jsonPrimitive?.contentOrNull,
                    )
                }

            val radioEndpoint = header["radioButton"]?.jsonObject
                ?.get("buttonRenderer")?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("watchPlaylistEndpoint")?.jsonObject?.let {
                    WatchEndpoint(
                        playlistId = it["playlistId"]?.jsonPrimitive?.contentOrNull,
                    )
                }

            val playlist = PlaylistItem(
                id = playlistId ?: "",
                title = title,
                author = author,
                songCountText = songCountText,
                thumbnail = thumbnail,
                description = description,
                playEndpoint = playEndpoint,
                shuffleEndpoint = shuffleEndpoint,
                radioEndpoint = radioEndpoint,
                isEditable = isEditable,
            )

            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val shelf = contents?.firstOrNull()?.jsonObject
                ?.get("musicPlaylistShelfRenderer")?.jsonObject

            val songs = shelf?.get("contents")?.jsonArray
                ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                ?.mapNotNull { fromMusicResponsiveListItemRenderer(it) }
                .orEmpty()

            val songsContinuation = shelf?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull

            val continuation = contents?.lastOrNull()?.jsonObject
                ?.get("musicPlaylistShelfRenderer")?.jsonObject
                ?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull
                ?: songsContinuation

            return PlaylistPage(
                playlist = playlist,
                songs = songs,
                songsContinuation = songsContinuation,
                continuation = continuation,
            )
        }

        fun fromPlaylistShelfContinuation(json: JsonObject): PlaylistPage? {
            val contContents = json["continuationContents"]?.jsonObject
                ?.get("musicPlaylistShelfContinuation")?.jsonObject ?: return null

            val playlistId = contContents["playlistId"]?.jsonPrimitive?.contentOrNull ?: ""

            val songs = contContents["contents"]?.jsonArray
                ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                ?.mapNotNull { fromMusicResponsiveListItemRenderer(it) }
                .orEmpty()

            val continuation = contContents["continuations"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull

            val playlist = PlaylistItem(
                id = playlistId,
                title = "",
            )

            return PlaylistPage(
                playlist = playlist,
                songs = songs,
                songsContinuation = continuation,
                continuation = continuation,
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: JsonObject): SongItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null

            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray

            val artists = mutableListOf<YTItem.Artist>()
            var album: YTItem.Album? = null

            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val navEp = run.jsonObject["navigationEndpoint"]?.jsonObject
                val browseId = navEp?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (browseId?.startsWith("UC") == true) {
                    artists.add(YTItem.Artist(name = text, id = browseId))
                } else if (browseId?.startsWith("MPRE") == true) {
                    album = YTItem.Album(name = text, id = browseId)
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
                id = videoId ?: return null,
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

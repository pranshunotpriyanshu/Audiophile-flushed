package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class ArtistItemsPage(
    val title: String,
    val items: List<YTItem>,
    val continuation: String?,
    val layout: ArtistItemsPageLayout,
) {
    enum class ArtistItemsPageLayout { GRID, LIST }

    companion object {
        fun fromBrowseResponse(json: JsonObject): ArtistItemsPage? {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return null

            val sectionContent = contents.firstOrNull()?.jsonObject ?: return null
            val gridRenderer = sectionContent["gridRenderer"]?.jsonObject
            if (gridRenderer != null) {
                val title = gridRenderer["header"]?.jsonObject
                    ?.get("gridHeaderRenderer")?.jsonObject
                    ?.get("title")?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()

                val items = gridRenderer["items"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["musicTwoRowItemRenderer"]?.jsonObject }
                    ?.mapNotNull { parseTwoRowItem(it) }
                    .orEmpty()

                val continuation = gridRenderer["continuations"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull

                return ArtistItemsPage(title, items, continuation, ArtistItemsPageLayout.GRID)
            }

            val playlistShelf = sectionContent["musicPlaylistShelfRenderer"]?.jsonObject
            val musicShelf = sectionContent["musicShelfRenderer"]?.jsonObject
            val shelfContents = playlistShelf?.get("contents")?.jsonArray
                ?: musicShelf?.get("contents")?.jsonArray.orEmpty()

            val title = json["header"]?.jsonObject
                ?.get("musicHeaderRenderer")?.jsonObject
                ?.get("title")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: musicShelf?.get("title")?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()

            val items = shelfContents.mapNotNull { item ->
                item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    ?.let { parseListItem(it) }
            }

            val continuation = playlistShelf?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull
                ?: musicShelf?.get("continuations")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull

            return ArtistItemsPage(title, items, continuation, ArtistItemsPageLayout.LIST)
        }

        fun fromMusicTwoRowItem(renderer: JsonObject): YTItem? {
            return parseTwoRowItem(renderer)
        }

        fun fromMusicResponsiveListItemRenderer(renderer: JsonObject): YTItem? {
            return parseListItem(renderer)
        }

        private fun parseTwoRowItem(renderer: JsonObject): YTItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val videoId = navEp?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            if (browseId?.startsWith("UC") == true) {
                return YTItem(
                    id = browseId, title = title, type = YTItem.Type.ARTIST,
                    thumbnailUrl = thumbnail, browseId = browseId,
                )
            }

            if (browseId?.startsWith("MPRE") == true || browseId?.startsWith("OLAK") == true) {
                return YTItem(
                    id = browseId, title = title, type = YTItem.Type.ALBUM,
                    thumbnailUrl = thumbnail, browseId = browseId,
                )
            }

            if (videoId != null) {
                val playlistId = navEp?.get("watchEndpoint")?.jsonObject
                    ?.get("playlistId")?.jsonPrimitive?.contentOrNull
                return YTItem(
                    id = videoId, title = title, type = YTItem.Type.SONG,
                    thumbnailUrl = thumbnail, playlistId = playlistId,
                )
            }

            return null
        }

        private fun parseListItem(renderer: JsonObject): YTItem? {
            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

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

            return YTItem(
                id = videoId,
                title = title,
                type = YTItem.Type.SONG,
                artists = artists,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                playlistId = playlistId,
            )
        }
    }
}

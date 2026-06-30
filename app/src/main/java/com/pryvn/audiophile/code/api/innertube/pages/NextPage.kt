package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class NextResult(
    val title: String?,
    val items: List<SongItem>,
    val currentIndex: Int?,
    val lyricsEndpoint: BrowseEndpoint?,
    val relatedEndpoint: BrowseEndpoint?,
    val continuation: String?,
    val endpoint: BrowseEndpoint?,
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: JsonObject): SongItem? {
        val title = renderer["title"]?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

        val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull ?: return null

        val artistRuns = renderer["longBylineText"]?.jsonObject
            ?.get("runs")?.jsonArray
        val artistName = artistRuns?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
        val artistId = artistRuns?.firstOrNull()?.jsonObject
            ?.get("navigationEndpoint")?.jsonObject
            ?.get("browseEndpoint")?.jsonObject
            ?.get("browseId")?.jsonPrimitive?.contentOrNull
        val artists = if (artistName != null) {
            listOf(YTItem.Artist(name = artistName, id = artistId))
        } else emptyList()

        val thumbnailUrl = renderer["thumbnail"]?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull

        val lengthText = renderer["lengthText"]?.jsonObject
            ?.get("simpleText")?.jsonPrimitive?.contentOrNull
        var durationSeconds: Int? = null
        lengthText?.let { text ->
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
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            playlistId = playlistId,
        )
    }

    fun fromNextResponse(json: JsonObject): NextResult {
        val contents = json["contents"]?.jsonObject
            ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
            ?.get("tabbedRenderer")?.jsonObject
            ?.get("watchNextTabbedResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray

        val tabContent = contents?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject

        val playlistPanel = tabContent?.get("musicQueueRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("playlistPanelRenderer")?.jsonObject

        val title = playlistPanel?.get("title")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?: playlistPanel?.get("titleText")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

        var currentIndex: Int? = null
        val items = playlistPanel?.get("contents")?.jsonArray
            ?.mapNotNull { content ->
                val renderer = content.jsonObject["playlistPanelVideoRenderer"]?.jsonObject
                if (renderer != null) {
                    if (renderer["selected"]?.jsonPrimitive?.booleanOrNull == true) {
                        currentIndex = content.jsonObject.values.indexOfFirst {
                            it is JsonObject && it.containsKey("playlistPanelVideoRenderer")
                        }
                    }
                    fromPlaylistPanelVideoRenderer(renderer)
                } else null
            }.orEmpty()

        val endpoint = json["currentVideoEndpoint"]?.jsonObject
            ?.get("watchEndpoint")?.jsonObject?.let {
                BrowseEndpoint(
                    browseId = it["browseId"]?.jsonPrimitive?.contentOrNull,
                    params = it["params"]?.jsonPrimitive?.contentOrNull,
                )
            }

        val continuation = playlistPanel?.get("continuations")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("continuationEndpoint")?.jsonObject
            ?.get("continuationCommand")?.jsonObject
            ?.get("token")?.jsonPrimitive?.contentOrNull

        val buttons = tabContent?.get("videoSecondaryInfoRenderer")?.jsonObject
            ?.get("videoActions")?.jsonObject
            ?.get("menuRenderer")?.jsonObject
            ?.get("topLevelButtons")?.jsonArray

        var lyricsEndpoint: BrowseEndpoint? = null
        var relatedEndpoint: BrowseEndpoint? = null

        buttons?.forEach { button ->
            val toggleBtn = button.jsonObject["toggleButtonRenderer"]?.jsonObject
            val navEp = toggleBtn?.get("defaultNavigationEndpoint")?.jsonObject
            val browseEp = navEp?.get("browseEndpoint")?.jsonObject
            val browseId = browseEp?.get("browseId")?.jsonPrimitive?.contentOrNull
            if (browseId == "FEmusic_library_header_pick") {
                lyricsEndpoint = BrowseEndpoint(
                    browseId = browseId,
                    params = browseEp["params"]?.jsonPrimitive?.contentOrNull,
                )
            } else if (browseId == "FEmusic_related") {
                relatedEndpoint = BrowseEndpoint(
                    browseId = browseId,
                    params = browseEp["params"]?.jsonPrimitive?.contentOrNull,
                )
            }
        }

        return NextResult(
            title = title,
            items = items,
            currentIndex = currentIndex,
            lyricsEndpoint = lyricsEndpoint,
            relatedEndpoint = relatedEndpoint,
            continuation = continuation,
            endpoint = endpoint,
        )
    }
}

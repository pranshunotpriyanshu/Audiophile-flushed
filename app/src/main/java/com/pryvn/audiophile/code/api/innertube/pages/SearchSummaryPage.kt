package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.YTItem
import kotlinx.serialization.json.*

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    companion object {
        fun fromSearchResponse(json: JsonObject): SearchSummaryPage {
            val contents = json["contents"]?.jsonObject
                ?.get("searchResultsRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val summaries = mutableListOf<SearchSummary>()

            contents?.forEach { content ->
                val section = content.jsonObject["musicShelfRenderer"]?.jsonObject ?: return@forEach
                val title = section["title"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach

                val shelfContents = section["contents"]?.jsonArray ?: return@forEach
                val items = shelfContents.mapNotNull { item ->
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: return@mapNotNull null
                    parseListItem(renderer)
                }

                summaries.add(SearchSummary(title = title, items = items))
            }

            return SearchSummaryPage(summaries = summaries)
        }

        fun parseListItem(renderer: JsonObject): YTItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: navEp?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull

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

            val artists = mutableListOf<YTItem.Artist>()
            var album: YTItem.Album? = null

            subtitleRuns?.forEach { run ->
                val text = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val ep = run.jsonObject["navigationEndpoint"]?.jsonObject
                val epBrowseId = ep?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                if (epBrowseId?.startsWith("UC") == true) {
                    artists.add(YTItem.Artist(name = text, id = epBrowseId))
                } else if (epBrowseId?.startsWith("MPRE") == true) {
                    album = YTItem.Album(name = text, id = epBrowseId)
                }
            }

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

            val id = browseId ?: videoId ?: return null
            val type = when {
                browseId?.startsWith("UC") == true -> YTItem.Type.ARTIST
                browseId?.startsWith("MPRE") == true -> YTItem.Type.ALBUM
                browseId?.startsWith("VL") == true -> YTItem.Type.PLAYLIST
                videoId != null -> YTItem.Type.VIDEO
                else -> YTItem.Type.SONG
            }

            return YTItem(
                id = id,
                title = title,
                type = type,
                artists = artists,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                browseId = browseId,
                playlistId = navEp?.get("watchEndpoint")?.jsonObject
                    ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
            )
        }
    }
}

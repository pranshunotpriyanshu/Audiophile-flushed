package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.YTItem
import kotlinx.serialization.json.*

enum class ChartType {
    TRENDING,
    TOP,
    GENRE,
    NEW_RELEASES,
}

data class ChartSection(
    val title: String,
    val items: List<YTItem>,
    val chartType: ChartType,
)

data class ChartsPage(
    val sections: List<ChartSection>,
    val continuation: String?,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject): ChartsPage {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?: json["contents"]?.jsonObject

            val sections = contents?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val chartSections = mutableListOf<ChartSection>()
            var continuation: String? = null

            sections?.forEach { section ->
                val carousel = section.jsonObject["musicCarouselShelfRenderer"]?.jsonObject ?: return@forEach
                val header = carousel["header"]?.jsonObject
                    ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject ?: return@forEach
                val title = header["title"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@forEach

                val chartType = when {
                    title.contains("Trending", ignoreCase = true) -> ChartType.TRENDING
                    title.contains("Top", ignoreCase = true) -> ChartType.TOP
                    title.contains("New Releases", ignoreCase = true) -> ChartType.NEW_RELEASES
                    title.contains("Genre", ignoreCase = true) -> ChartType.GENRE
                    else -> ChartType.TRENDING
                }

                val carouselContents = carousel["contents"]?.jsonArray ?: return@forEach
                val items = carouselContents.mapNotNull { content ->
                    val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                    val listItem = content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    when {
                        twoRow != null -> parseTwoRowItem(twoRow)
                        listItem != null -> parseListItem(listItem)
                        else -> null
                    }
                }

                chartSections.add(ChartSection(title = title, items = items, chartType = chartType))
            }

            val shelf = contents?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("musicShelfRenderer")?.jsonObject
            continuation = shelf?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("continuation")?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull

            return ChartsPage(sections = chartSections, continuation = continuation)
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

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val subtitle = subtitleRuns?.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }?.takeIf { it.isNotBlank() }

            val thumbnailUrl = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val id = browseId ?: videoId ?: return null
            val type = when {
                browseId?.startsWith("UC") == true -> YTItem.Type.ARTIST
                browseId?.startsWith("MPRE") == true -> YTItem.Type.ALBUM
                browseId?.startsWith("VL") == true -> YTItem.Type.PLAYLIST
                videoId != null -> YTItem.Type.VIDEO
                else -> YTItem.Type.SONG
            }

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

            return YTItem(
                id = id,
                title = title,
                type = type,
                artists = artists,
                album = album,
                thumbnailUrl = thumbnailUrl,
                browseId = browseId,
                playlistId = navEp?.get("watchEndpoint")?.jsonObject
                    ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
            )
        }

        private fun parseListItem(renderer: JsonObject): YTItem? {
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
                thumbnailUrl = thumbnailUrl,
                browseId = browseId,
                playlistId = navEp?.get("watchEndpoint")?.jsonObject
                    ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
            )
        }
    }
}

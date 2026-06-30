package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.YTItem
import kotlinx.serialization.json.*

data class LibraryPage(
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject): LibraryPage {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?: json["contents"]?.jsonObject

            val sections = contents?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val items = mutableListOf<YTItem>()
            var continuation: String? = null

            sections?.forEach { section ->
                val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject ?: return@forEach
                val shelfContents = shelf["contents"]?.jsonArray ?: return@forEach
                shelfContents.forEach { content ->
                    val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                    val listItem = content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    when {
                        twoRow != null -> parseTwoRowItem(twoRow)?.let { items.add(it) }
                        listItem != null -> parseListItem(listItem)?.let { items.add(it) }
                    }
                }
                val shelfContinuation = shelf["continuations"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("continuation")?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull
                if (shelfContinuation != null) continuation = shelfContinuation
            }

            return LibraryPage(items = items, continuation = continuation)
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

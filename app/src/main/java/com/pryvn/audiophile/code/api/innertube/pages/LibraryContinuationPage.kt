package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.YTItem
import kotlinx.serialization.json.*

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject): LibraryContinuationPage {
            val contObj = json["continuationContents"]?.jsonObject
                ?: json["contents"]?.jsonObject?.get("sectionListContinuation")?.jsonObject
                ?: return LibraryContinuationPage(emptyList(), null)

            val sectionContinuation = contObj.get("sectionListContinuation")?.jsonObject
            val gridContinuation = contObj.get("gridContinuation")?.jsonObject
            val shelfContinuation = contObj.get("musicShelfContinuation")?.jsonObject
            val playlistShelfContinuation = contObj.get("musicPlaylistShelfContinuation")?.jsonObject

            val items = mutableListOf<YTItem>()
            var continuation: String? = null

            sectionContinuation?.get("contents")?.jsonArray?.forEach { section ->
                val sectionContent = section.jsonObject
                sectionContent["musicCarouselShelfRenderer"]?.jsonObject?.let { carousel ->
                    carousel["contents"]?.jsonArray?.forEach { content ->
                        content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                            ?.let { parseTwoRowItem(it) }?.let { items.add(it) }
                        content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                            ?.let { parseListItem(it) }?.let { items.add(it) }
                    }
                }
                sectionContent["musicShelfRenderer"]?.jsonObject?.let { shelf ->
                    shelf["contents"]?.jsonArray?.forEach { content ->
                        content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                            ?.let { parseListItem(it) }?.let { items.add(it) }
                    }
                }
            }

            gridContinuation?.get("items")?.jsonArray?.forEach { item ->
                item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                    ?.let { parseTwoRowItem(it) }?.let { items.add(it) }
            }

            shelfContinuation?.get("contents")?.jsonArray?.forEach { content ->
                content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    ?.let { parseListItem(it) }?.let { items.add(it) }
            }

            playlistShelfContinuation?.get("contents")?.jsonArray?.forEach { content ->
                content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    ?.let { parseListItem(it) }?.let { items.add(it) }
            }

            continuation = sectionContinuation?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull
                ?: gridContinuation?.get("continuations")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull
                ?: shelfContinuation?.get("continuations")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull
                ?: playlistShelfContinuation?.get("continuations")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.contentOrNull

            return LibraryContinuationPage(
                items = if (items.isEmpty()) emptyList() else items,
                continuation = if (items.isEmpty()) null else continuation,
            )
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

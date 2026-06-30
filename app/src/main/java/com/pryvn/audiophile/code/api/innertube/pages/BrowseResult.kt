package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class BrowseResult(
    val title: String?,
    val thumbnail: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )

    companion object {
        fun fromBrowseResponse(json: JsonObject): BrowseResult {
            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            val browseItems = contents?.mapNotNull { content ->
                val sectionContent = content.jsonObject

                val gridRenderer = sectionContent["gridRenderer"]?.jsonObject
                if (gridRenderer != null) {
                    val sectionTitle = gridRenderer["header"]?.jsonObject
                        ?.get("gridHeaderRenderer")?.jsonObject
                        ?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    val items = gridRenderer["items"]?.jsonArray
                        ?.mapNotNull { it.jsonObject["musicTwoRowItemRenderer"]?.jsonObject }
                        ?.mapNotNull { parseTwoRowItem(it) }
                        .orEmpty()
                    return@mapNotNull Item(sectionTitle, items)
                }

                val carousel = sectionContent["musicCarouselShelfRenderer"]?.jsonObject
                if (carousel != null) {
                    val sectionTitle = carousel["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                        ?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    val items = carousel["contents"]?.jsonArray
                        ?.mapNotNull { it.jsonObject["musicTwoRowItemRenderer"]?.jsonObject }
                        ?.mapNotNull { parseTwoRowItem(it) }
                        .orEmpty()
                    return@mapNotNull Item(sectionTitle, items)
                }

                null
            }.orEmpty()

            val header = json["header"]?.jsonObject
            val title = header?.get("musicHeaderRenderer")?.jsonObject
                ?.get("title")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

            val thumbnail = header?.get("musicImmersiveHeaderRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull
                ?: header?.get("musicVisualHeaderRenderer")?.jsonObject
                    ?.get("foregroundThumbnail")?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
                ?: header?.get("musicDetailHeaderRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
                ?: header?.get("musicResponsiveHeaderRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
                ?: header?.get("musicHeaderRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
                ?: browseItems
                    .asSequence()
                    .flatMap { it.items.asSequence() }
                    .mapNotNull { it.thumbnailUrl }
                    .firstOrNull()

            return BrowseResult(title, thumbnail, browseItems)
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

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
            val subtitleText = subtitleRuns?.joinToString("") {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }?.trim()

            if (browseId?.startsWith("UC") == true) {
                return YTItem(
                    id = browseId,
                    title = title,
                    type = YTItem.Type.ARTIST,
                    thumbnailUrl = thumbnail,
                    browseId = browseId,
                )
            }

            if (browseId?.startsWith("MPRE") == true || browseId?.startsWith("OLAK") == true) {
                val artists = subtitleText?.split(" • ")?.firstOrNull()
                val year = subtitleText?.split(" • ")?.lastOrNull()?.takeIf { it.all { c -> c.isDigit() } }
                return YTItem(
                    id = browseId,
                    title = title,
                    type = YTItem.Type.ALBUM,
                    thumbnailUrl = thumbnail,
                    browseId = browseId,
                )
            }

            if (browseId?.startsWith("VL") == true) {
                return YTItem(
                    id = browseId,
                    title = title,
                    type = YTItem.Type.PLAYLIST,
                    thumbnailUrl = thumbnail,
                    browseId = browseId,
                )
            }

            if (videoId != null) {
                return YTItem(
                    id = videoId,
                    title = title,
                    type = YTItem.Type.VIDEO,
                    thumbnailUrl = thumbnail,
                    playlistId = navEp?.get("watchEndpoint")?.jsonObject
                        ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
                )
            }

            return null
        }
    }
}

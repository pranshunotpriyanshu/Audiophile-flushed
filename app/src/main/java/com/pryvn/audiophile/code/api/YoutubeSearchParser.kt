package com.pryvn.audiophile.code.api

import kotlinx.serialization.json.*

internal object YouTubeSearchParser {

    fun parseSearchResults(root: JsonObject): YTSearchResult {
        val contents = root["contents"]?.jsonObject
            ?.get("tabbedSearchResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?: return YTSearchResult(emptyList())

        val items = mutableListOf<YTSongItem>()
        var continuation: String? = null

        for (sectionIdx in 0 until contents.size) {
            val section = contents[sectionIdx].jsonObject
            val shelf = section["musicShelfRenderer"]?.jsonObject
            if (shelf != null) {
                val shelfContents = shelf["contents"]?.jsonArray ?: continue
                for (content in shelfContents) {
                    val renderer = content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                    parseListItem(renderer)?.let { items.add(it) }
                }
                val cont = shelf["continuations"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.content
                if (cont != null) continuation = cont
            }

            val itemSection = section["itemSectionRenderer"]?.jsonObject
            if (itemSection != null) {
                val sectionContents = itemSection["contents"]?.jsonArray ?: continue
                for (content in sectionContents) {
                    val inlineRenderer = content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                    if (inlineRenderer != null) {
                        parseListItem(inlineRenderer)?.let { items.add(it) }
                        continue
                    }
                    val shelf2 = content.jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                    val shelfContents = shelf2["contents"]?.jsonArray ?: continue
                    for (shelfContent in shelfContents) {
                        val renderer = shelfContent.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
                        parseListItem(renderer)?.let { items.add(it) }
                    }
                }
            }
        }

        return YTSearchResult(items.distinctBy { it.videoId }, continuation)
    }

    fun parseSearchContinuation(root: JsonObject): YTSearchResult {
        val continuationContents = root["continuationContents"]?.jsonObject
            ?.get("musicShelfContinuation")?.jsonObject ?: return YTSearchResult(emptyList())

        val items = mutableListOf<YTSongItem>()
        val contents = continuationContents["contents"]?.jsonArray ?: return YTSearchResult(emptyList())

        for (content in contents) {
            val renderer = content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: continue
            parseListItem(renderer)?.let { items.add(it) }
        }

        val continuation = continuationContents["continuations"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("nextContinuationData")?.jsonObject
            ?.get("continuation")?.jsonPrimitive?.content

        return YTSearchResult(items, continuation)
    }

    fun parseListItem(renderer: JsonObject): YTSongItem? {
        val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
        val fixedColumns = renderer["fixedColumns"]?.jsonArray

        val title = flexColumns.firstOrNull()?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.joinToString("") { run ->
                run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
            }?.trim() ?: return null

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

        val artists = mutableListOf<YTArtist>()
        var album: YTAlbum? = null
        var thumbnailUrl: String? = null

        subtitleRuns?.forEach { run ->
            val runObj = run.jsonObject
            val text = runObj["text"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val navEndpoint = runObj["navigationEndpoint"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
            val browseId = navEndpoint?.get("browseId")?.jsonPrimitive?.contentOrNull

            if (browseId?.startsWith("UC") == true) {
                artists.add(YTArtist(text, browseId))
            } else if (browseId?.startsWith("MPRE") == true) {
                album = YTAlbum(text, browseId)
            }
        }

        val thumbnails = renderer["thumbnail"]?.jsonObject
            ?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject
            ?.get("thumbnails")?.jsonArray

        thumbnailUrl = thumbnails?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull

        var durationSeconds: Int? = null
        fixedColumns?.firstOrNull()?.jsonObject
            ?.get("musicResponsiveListItemFixedColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?.let { durationSeconds = parseDuration(it) }

        return YTSongItem(
            videoId = videoId,
            title = title,
            artists = artists,
            album = album,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            playlistId = playlistId,
        )
    }

    private fun parseDuration(text: String): Int? {
        val parts = text.split(":")
        return when (parts.size) {
            2 -> parts[0].toIntOrNull()?.let { m ->
                parts[1].toIntOrNull()?.let { s -> m * 60 + s }
            }
            3 -> parts[0].toIntOrNull()?.let { h ->
                parts[1].toIntOrNull()?.let { m ->
                    parts[2].toIntOrNull()?.let { s -> h * 3600 + m * 60 + s }
                }
            }
            else -> null
        }
    }
}
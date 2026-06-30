package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.SongItem
import com.pryvn.audiophile.code.api.innertube.models.YTItem
import kotlinx.serialization.json.*

data class HistorySection(
    val title: String,
    val songs: List<SongItem>,
)

data class HistoryPage(
    val sections: List<HistorySection>?,
) {
    companion object {
        fun fromSectionListContent(json: JsonObject): HistoryPage {
            val contents = json["contents"]?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray ?: return HistoryPage(sections = null)

            val sections = contents.mapNotNull { content ->
                val shelf = content.jsonObject["musicShelfRenderer"]?.jsonObject ?: return@mapNotNull null
                val title = shelf["title"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                val shelfContents = shelf["contents"]?.jsonArray ?: return@mapNotNull null
                val songs = shelfContents.mapNotNull { item ->
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject ?: return@mapNotNull null
                    parseSongItem(renderer)
                }

                HistorySection(title = title, songs = songs)
            }

            return HistoryPage(sections = sections)
        }

        private fun parseSongItem(renderer: JsonObject): SongItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val playlistId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

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

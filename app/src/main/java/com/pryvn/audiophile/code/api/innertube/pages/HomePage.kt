package com.pryvn.audiophile.code.api.innertube.pages

import kotlinx.serialization.json.*

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String?,
) {
    data class Section(
        val title: String,
        val items: List<Item>,
    ) {
        data class Item(
            val browseId: String?,
            val title: String,
            val subtitle: String?,
            val thumbnailUrl: String?,
            val videoId: String?,
            val playlistId: String?,
        )

        companion object {
            fun fromMusicCarouselShelfRenderer(json: JsonObject): Section? {
                val header = json["header"]?.jsonObject
                    ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                    ?: return null
                val title = header["title"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

                val contents = json["contents"]?.jsonArray ?: return null
                val items = contents.mapNotNull { content ->
                    val twoRow = content.jsonObject["musicTwoRowItemRenderer"]?.jsonObject ?: return@mapNotNull null
                    val itemTitle = twoRow["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val thumbnail = twoRow["thumbnailRenderer"]?.jsonObject
                        ?.get("musicThumbnailRenderer")?.jsonObject
                        ?.get("thumbnail")?.jsonObject
                        ?.get("thumbnails")?.jsonArray
                        ?.lastOrNull()?.jsonObject
                        ?.get("url")?.jsonPrimitive?.contentOrNull
                    val navEp = twoRow["navigationEndpoint"]?.jsonObject
                    val browseId = navEp?.get("browseEndpoint")?.jsonObject
                        ?.get("browseId")?.jsonPrimitive?.contentOrNull
                    val videoId = navEp?.get("watchEndpoint")?.jsonObject
                        ?.get("videoId")?.jsonPrimitive?.contentOrNull
                    val playlistId = navEp?.get("watchEndpoint")?.jsonObject
                        ?.get("playlistId")?.jsonPrimitive?.contentOrNull
                    val subtitleRuns = twoRow["subtitle"]?.jsonObject
                        ?.get("runs")?.jsonArray
                    val subtitle = subtitleRuns?.joinToString("") {
                        it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
                    }?.takeIf { it.isNotBlank() }

                    Item(
                        browseId = browseId,
                        title = itemTitle,
                        subtitle = subtitle,
                        thumbnailUrl = thumbnail,
                        videoId = videoId,
                        playlistId = playlistId,
                    )
                }

                return Section(title = title, items = items)
            }
        }
    }

    data class Chip(
        val text: String,
        val browseId: String?,
        val params: String?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(json: JsonObject): Chip? {
                val text = json["text"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null
                val navEp = json["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                return Chip(
                    text = text,
                    browseId = navEp?.get("browseId")?.jsonPrimitive?.contentOrNull,
                    params = navEp?.get("params")?.jsonPrimitive?.contentOrNull,
                )
            }
        }
    }
}

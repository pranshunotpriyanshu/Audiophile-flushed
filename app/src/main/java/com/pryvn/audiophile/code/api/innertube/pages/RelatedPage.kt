package com.pryvn.audiophile.code.api.innertube.pages

import com.pryvn.audiophile.code.api.innertube.models.*
import kotlinx.serialization.json.*

data class RelatedPage(
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val playlists: List<PlaylistItem>,
) {
    companion object {
        fun fromBrowseResponse(json: JsonObject): RelatedPage {
            val songs = mutableListOf<SongItem>()
            val albums = mutableListOf<AlbumItem>()
            val artists = mutableListOf<ArtistItem>()
            val playlists = mutableListOf<PlaylistItem>()

            val contents = json["contents"]?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            contents?.forEach { section ->
                val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
                val carousel = section.jsonObject["musicCarouselShelfRenderer"]?.jsonObject

                if (shelf != null) {
                    val shelfTitle = shelf["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull

                    val shelfContents = shelf["contents"]?.jsonArray ?: return@forEach
                    when {
                        shelfTitle?.contains("Song", ignoreCase = true) == true ||
                        shelfTitle?.contains("Video", ignoreCase = true) == true -> {
                            shelfContents.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                                .mapNotNull { parseSongItem(it) }
                                .let { songs.addAll(it) }
                        }
                    }
                }

                if (carousel != null) {
                    val header = carousel["header"]?.jsonObject
                        ?.get("musicCarouselShelfBasicHeaderRenderer")?.jsonObject
                    val sectionTitle = header?.get("title")?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull

                    val carouselContents = carousel["contents"]?.jsonArray ?: return@forEach
                    when {
                        sectionTitle?.contains("Album", ignoreCase = true) == true -> {
                            carouselContents.mapNotNull { it.jsonObject["musicTwoRowItemRenderer"]?.jsonObject }
                                .mapNotNull { parseAlbumItem(it) }
                                .let { albums.addAll(it) }
                        }
                        sectionTitle?.contains("Artist", ignoreCase = true) == true -> {
                            carouselContents.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                                .mapNotNull { parseArtistItem(it) }
                                .let { artists.addAll(it) }
                        }
                        sectionTitle?.contains("Playlist", ignoreCase = true) == true -> {
                            carouselContents.mapNotNull { it.jsonObject["musicTwoRowItemRenderer"]?.jsonObject }
                                .mapNotNull { parsePlaylistItem(it) }
                                .let { playlists.addAll(it) }
                        }
                        sectionTitle?.contains("Song", ignoreCase = true) == true ||
                        sectionTitle?.contains("Video", ignoreCase = true) == true -> {
                            carouselContents.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                                .mapNotNull { parseSongItem(it) }
                                .let { songs.addAll(it) }
                        }
                    }
                }
            }

            return RelatedPage(
                songs = songs,
                albums = albums,
                artists = artists,
                playlists = playlists,
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: JsonObject): YTItem? {
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
            if (browseId?.startsWith("VL") == true) {
                return YTItem(
                    id = browseId, title = title, type = YTItem.Type.PLAYLIST,
                    thumbnailUrl = thumbnail, browseId = browseId,
                )
            }
            if (videoId != null) {
                return YTItem(
                    id = videoId, title = title, type = YTItem.Type.VIDEO,
                    thumbnailUrl = thumbnail,
                    playlistId = navEp?.get("watchEndpoint")?.jsonObject
                        ?.get("playlistId")?.jsonPrimitive?.contentOrNull,
                )
            }
            return null
        }

        private fun parseSongItem(renderer: JsonObject): SongItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null

            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.trim() ?: return null

            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

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

        private fun parseAlbumItem(renderer: JsonObject): AlbumItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val subtitleRuns = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray

            val artists = subtitleRuns
                ?.filter { it.jsonObject["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull
                    ?.startsWith("UC") == true }
                ?.joinToString(", ") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.takeIf { it.isNotBlank() }

            val year = subtitleRuns
                ?.lastOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.all { c -> c.isDigit() } }

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val explicit = renderer["subtitleBadges"]?.jsonArray
                ?.any { badge ->
                    badge.jsonObject["musicInlineBadgeRenderer"]?.jsonObject
                        ?.get("icon")?.jsonObject
                        ?.get("iconType")?.jsonPrimitive?.contentOrNull == "MUSIC_EXPLICIT_BADGE"
                } == true

            val playlistId = navEp?.get("watchEndpoint")?.jsonObject
                ?.get("playlistId")?.jsonPrimitive?.contentOrNull

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = title,
                artists = artists,
                year = year,
                thumbnail = thumbnail,
                explicit = explicit,
            )
        }

        private fun parseArtistItem(renderer: JsonObject): ArtistItem? {
            val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null

            val title = flexColumns.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val thumbnail = renderer["thumbnail"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            val subscriberCountText = flexColumns.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                ?.takeIf { it.isNotBlank() }

            return ArtistItem(
                id = browseId,
                title = title,
                thumbnail = thumbnail,
                channelId = browseId,
                subscriberCountText = subscriberCountText,
            )
        }

        private fun parsePlaylistItem(renderer: JsonObject): PlaylistItem? {
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

            val navEp = renderer["navigationEndpoint"]?.jsonObject
            val browseId = navEp?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull

            val authorRuns = renderer["shortBylineText"]?.jsonObject
                ?.get("runs")?.jsonArray
            val authorName = authorRuns?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            val authorId = authorRuns?.firstOrNull()?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
            val author = if (authorName != null) {
                YTItem.Artist(name = authorName, id = authorId)
            } else null

            val songCountText = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull

            val thumbnail = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

            return PlaylistItem(
                id = browseId ?: return null,
                title = title,
                author = author,
                songCountText = songCountText,
                thumbnail = thumbnail,
            )
        }
    }
}

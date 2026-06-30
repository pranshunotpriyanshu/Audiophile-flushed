package com.pryvn.audiophile.code.api.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*
import com.pryvn.audiophile.code.api.innertube.models.*
import com.pryvn.audiophile.code.api.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.pryvn.audiophile.code.api.innertube.models.YouTubeClient.Companion.IOS
import com.pryvn.audiophile.code.api.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.pryvn.audiophile.code.api.innertube.pages.*
import com.pryvn.audiophile.code.api.innertube.utils.getTranscriptParams

object YouTube {
    private val innerTube = InnerTube()
    private val mutableAuthState = kotlinx.coroutines.flow.MutableStateFlow(PlaybackAuthState.EMPTY)
    val authStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackAuthState> = mutableAuthState.asStateFlow()

    var authState: PlaybackAuthState
        get() = mutableAuthState.value
        set(value) {
            val normalized = value.normalized()
            mutableAuthState.value = normalized
            innerTube.applyAuthState(normalized)
        }

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) { innerTube.locale = value }

    var visitorData: String?
        get() = authState.visitorData
        set(value) { authState = authState.copy(visitorData = value) }

    var dataSyncId: String?
        get() = authState.dataSyncId
        set(value) { authState = authState.copy(dataSyncId = value) }

    var cookie: String?
        get() = authState.cookie
        set(value) { authState = authState.copy(cookie = value) }

    var poToken: String?
        get() = authState.poToken
        set(value) { authState = authState.copy(poToken = value) }

    fun hasLoginCookie(): Boolean = authState.hasLoginCookie

    private val VISITOR_DATA_REGEX = Regex("""C[a-zA-Z0-9_-]{22,}""")

    private fun generateVisitorData(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-"
        val randomStr = (1..48).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
        return "Cgt" + randomStr.take(42) + "Vw"
    }

    suspend fun ensureVisitorData() {
        if (!visitorData.isNullOrBlank()) return
        fetchVisitorData().onSuccess { visitorData = it }
            .onFailure { visitorData = generateVisitorData() }
    }

    suspend fun fetchVisitorData(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = innerTube.getSwJsData()
            val body = response.bodyAsText().removePrefix(")]}'")
            val root = Json.parseToJsonElement(body).jsonArray
            root[0].jsonArray[2].jsonArray
                .first { element ->
                    (element as? JsonPrimitive)?.contentOrNull?.let { candidate ->
                        VISITOR_DATA_REGEX.containsMatchIn(candidate)
                    } == true
                }
                .jsonPrimitive.content
        }
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        runCatching {
            val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<JsonObject>()
            val contents = response["contents"]?.jsonArray ?: JsonArray(emptyList())
            val queries = contents.firstOrNull()?.jsonObject
                ?.get("searchSuggestionsSectionRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
                ?.mapNotNull { content ->
                    content.jsonObject["searchSuggestionRenderer"]?.jsonObject
                        ?.get("suggestion")?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.joinToString(separator = "") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
                }.orEmpty()
            SearchSuggestions(queries = queries, recommendedItems = emptyList())
        }

    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.SONGS,
        useAccountContext: Boolean = true,
    ): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(
                client = WEB_REMIX,
                query = query,
                params = filter.value,
                useAccountContext = useAccountContext,
            ).body<JsonObject>()
            parseSearchResults(response)
        }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(
                client = WEB_REMIX,
                continuation = continuation,
            ).body<JsonObject>()
            val continuationContents = response["continuationContents"]?.jsonObject
                ?.get("musicShelfContinuation")?.jsonObject ?: return@runCatching SearchResult(emptyList(), null)
            val items = continuationContents["contents"]?.jsonArray
                ?.mapNotNull { content ->
                    content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?.let { SearchPage.toYTItem(it) }
                }.orEmpty()
            val nextCont = continuationContents["continuations"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.content
            SearchResult(items = items, continuation = if (items.isEmpty()) null else nextCont)
        }

    private fun parseSearchResults(root: JsonObject): SearchResult {
        val contents = root["contents"]?.jsonObject
            ?.get("tabbedSearchResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?: return SearchResult(emptyList())

        val items = mutableListOf<SearchPage>()
        var continuation: String? = null

        for (section in contents) {
            val shelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
            if (shelf != null) {
                val shelfContents = shelf["contents"]?.jsonArray ?: continue
                for (content in shelfContents) {
                    content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?.let { SearchPage.toYTItem(it) }?.let { items.add(it) }
                }
                continuation = shelf["continuations"]?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.content
            }

            val itemSection = section.jsonObject["itemSectionRenderer"]?.jsonObject
            if (itemSection != null) {
                val sectionContents = itemSection["contents"]?.jsonArray ?: continue
                for (content in sectionContents) {
                    content.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?.let { SearchPage.toYTItem(it) }?.let { items.add(it) }
                    val shelf2 = content.jsonObject["musicShelfRenderer"]?.jsonObject ?: continue
                    val shelfContents = shelf2["contents"]?.jsonArray ?: continue
                    for (sc in shelfContents) {
                        sc.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                            ?.let { SearchPage.toYTItem(it) }?.let { items.add(it) }
                    }
                }
            }
        }

        return SearchResult(items.distinctBy { it.videoId }, continuation)
    }

    suspend fun home(continuation: String? = null, params: String? = null): Result<HomePage> =
        runCatching {
            if (continuation != null) {
                return@runCatching homeContinuation(continuation).getOrThrow()
            }
            val response = innerTube.browse(
                client = WEB_REMIX,
                browseId = "FEmusic_home",
                params = params,
                setLogin = hasLoginCookie(),
            ).body<JsonObject>()
            parseHomePage(response)
        }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> =
        runCatching {
            val response = innerTube.browse(
                client = WEB_REMIX,
                continuation = continuation,
            ).body<JsonObject>()
            val contContents = response["continuationContents"]?.jsonObject
                ?.get("sectionListContinuation")?.jsonObject
            val sections = contContents?.get("contents")?.jsonArray
                ?.mapNotNull {
                    it.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                        ?.let { HomePage.Section.fromMusicCarouselShelfRenderer(it) }
                }.orEmpty()
            val nextCont = if (sections.isEmpty()) null else {
                contContents?.get("continuations")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("nextContinuationData")?.jsonObject
                    ?.get("continuation")?.jsonPrimitive?.content
            }
            HomePage(chips = null, sections = sections, continuation = nextCont)
        }

    private fun parseHomePage(root: JsonObject): HomePage {
        val sectionList = root["contents"]?.jsonObject
            ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?: return HomePage(chips = null, sections = emptyList(), continuation = null)

        val continuation = sectionList["continuations"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("nextContinuationData")?.jsonObject
            ?.get("continuation")?.jsonPrimitive?.content

        val sections = sectionList["contents"]?.jsonArray
            ?.mapNotNull {
                it.jsonObject["musicCarouselShelfRenderer"]?.jsonObject
                    ?.let { HomePage.Section.fromMusicCarouselShelfRenderer(it) }
            }.orEmpty()

        val chips = sectionList["header"]?.jsonObject
            ?.get("chipCloudRenderer")?.jsonObject
            ?.get("chips")?.jsonArray
            ?.mapNotNull {
                it.jsonObject["chipCloudChipRenderer"]?.jsonObject
                    ?.let { HomePage.Chip.fromChipCloudChipRenderer(it) }
            }

        return HomePage(chips = chips, sections = sections, continuation = continuation)
    }

    suspend fun artist(browseId: String): Result<JsonObject> = runCatching {
        innerTube.browse(WEB_REMIX, browseId).body<JsonObject>()
    }

    suspend fun album(browseId: String): Result<JsonObject> = runCatching {
        innerTube.browse(WEB_REMIX, browseId).body<JsonObject>()
    }

    suspend fun playlist(playlistId: String): Result<JsonObject> = runCatching {
        innerTube.browse(WEB_REMIX, "VL$playlistId", setLogin = hasLoginCookie()).body<JsonObject>()
    }

    suspend fun fetchAccountInfo(): Result<AccountInfo> = runCatching {
        val result = innerTube.accountMenu(WEB_REMIX).body<JsonObject>()
        val header = result["actions"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("openPopupAction")?.jsonObject
            ?.get("popup")?.jsonObject
            ?.get("accountMenuPopupRenderer")?.jsonObject
            ?.get("header")?.jsonObject
            ?.get("activeAccountHeaderRenderer")?.jsonObject
        val name = header?.get("accountName")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
        val email = header?.get("email")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
        val channelHandle = header?.get("channelHandle")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
        val avatarUrl = header?.get("accountPhoto")?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull
        AccountInfo(name = name, email = email, channelHandle = channelHandle, avatarUrl = avatarUrl)
    }

    suspend fun library(browseId: String = "FEmusic_liked_playlists"): Result<JsonObject> = runCatching {
        innerTube.browse(WEB_REMIX, browseId = browseId, setLogin = hasLoginCookie()).body<JsonObject>()
    }

    suspend fun next(videoId: String, playlistId: String? = null): Result<JsonObject> = runCatching {
        innerTube.next(WEB_REMIX, videoId, playlistId).body<JsonObject>()
    }

    @Volatile
    private var signatureTimestamp: Int = 0

    suspend fun fetchSignatureTimestamp(): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val jsDataUrl = java.net.URL("https://music.youtube.com/sw.js_data")
            val jsDataConn = jsDataUrl.openConnection().apply {
                setRequestProperty("User-Agent", WEB_REMIX.userAgent)
            }
            val jsDataBody = jsDataConn.getInputStream().bufferedReader().readText().removePrefix(")]}'")
            val root = Json.parseToJsonElement(jsDataBody).jsonArray
            val scripts = root[0].jsonArray[2].jsonArray
            val baseJsRelativeUrl = scripts.filterIsInstance<JsonPrimitive>()
                .firstOrNull { it.content.contains("base.js") }
                ?.content ?: throw IllegalStateException("Could not find base.js URL")
            val baseJsUrl = if (baseJsRelativeUrl.startsWith("http")) baseJsRelativeUrl
                else "https://music.youtube.com$baseJsRelativeUrl"

            val baseJsConn = java.net.URL(baseJsUrl).openConnection().apply {
                setRequestProperty("User-Agent", WEB_REMIX.userAgent)
            }
            val baseJs = baseJsConn.getInputStream().bufferedReader().readText()
            val match = Regex("""signatureTimestamp[=:]\s*(\d{5})""").find(baseJs)
                ?: Regex("""var\s+sig\s*=\s*\{[^}]*ST:\s*(\d{5})""").find(baseJs)
                ?: throw IllegalStateException("Could not extract signature timestamp from base.js")
            match.groupValues[1].toInt()
        }.also { signatureTimestamp = it }
    }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        useIos: Boolean = false,
    ): Result<JsonObject> = runCatching {
        ensureVisitorData()
        val clients = if (useIos) {
            listOf(IOS)
        } else {
            val st = if (signatureTimestamp > 0) signatureTimestamp else {
                try { fetchSignatureTimestamp().getOrNull() ?: 0 } catch (_: Throwable) { 0 }
            }
            listOf(
                WEB_REMIX.copy(useSignatureTimestamp = st > 0),
                IOS,
                ANDROID_MUSIC,
            )
        }

        var lastException: Exception? = null
        for (client in clients) {
            try {
                val st = if (client.useSignatureTimestamp && signatureTimestamp > 0) signatureTimestamp else null
                val response = innerTube.player(
                    client = client,
                    videoId = videoId,
                    playlistId = playlistId,
                    signatureTimestamp = st,
                    authState = authState,
                    includeDataSyncId = true,
                ).body<JsonObject>()

                val playabilityStatus = response["playabilityStatus"]?.jsonObject
                val status = playabilityStatus?.get("status")?.jsonPrimitive?.contentOrNull
                if (status == "OK") return@runCatching response

                if (status == "LOGIN_REQUIRED") {
                    try {
                        val retryResponse = innerTube.player(
                            client = client,
                            videoId = videoId,
                            playlistId = playlistId,
                            signatureTimestamp = st,
                            authState = authState,
                            includeDataSyncId = false,
                        ).body<JsonObject>()
                        val retryStatus = retryResponse["playabilityStatus"]?.jsonObject
                            ?.get("status")?.jsonPrimitive?.contentOrNull
                        if (retryStatus == "OK") return@runCatching retryResponse
                    } catch (_: Throwable) {}
                }

                lastException = IllegalStateException("Player returned status: $status for client ${client.clientName}")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastException = e
            }
        }
        throw lastException ?: IllegalStateException("All player clients failed")
    }

    suspend fun registerPlayback(videoId: String): Result<Unit> = runCatching {
        innerTube.registerPlayback(WEB_REMIX, videoId, authState).body<JsonObject>()
    }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query).body<JsonObject>()
        val contents = response["contents"]?.jsonObject
            ?.get("tabbedSearchResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray.orEmpty()

        val topItems = mutableListOf<com.pryvn.audiophile.code.api.innertube.models.YTItem>()
        val summaries = mutableListOf<com.pryvn.audiophile.code.api.innertube.pages.SearchSummary>()

        contents.forEach { content ->
            val sectionObj = content.jsonObject
            sectionObj["musicShelfRenderer"]?.jsonObject?.let { shelf ->
                val shelfTitle = shelf["title"]?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@let
                val shelfItems = shelf["contents"]?.jsonArray
                    ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                    ?.mapNotNull { SearchSummaryPage.parseListItem(it) }
                    .orEmpty()
                if (shelfItems.isNotEmpty()) {
                    summaries.add(com.pryvn.audiophile.code.api.innertube.pages.SearchSummary(title = shelfTitle, items = shelfItems))
                }
            }
            sectionObj["itemSectionRenderer"]?.jsonObject?.get("contents")?.jsonArray?.let { sectionContents ->
                sectionContents.forEach { item ->
                    item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?.let { SearchSummaryPage.parseListItem(it) }?.let { topItems.add(it) }
                    item.jsonObject["musicShelfRenderer"]?.jsonObject?.let { shelf ->
                        val shelfTitle = shelf["title"]?.jsonObject
                            ?.get("runs")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@let
                        val shelfItems = shelf["contents"]?.jsonArray
                            ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                            ?.mapNotNull { SearchSummaryPage.parseListItem(it) }
                            .orEmpty()
                        if (shelfItems.isNotEmpty()) {
                            summaries.add(com.pryvn.audiophile.code.api.innertube.pages.SearchSummary(title = shelfTitle, items = shelfItems))
                        }
                    }
                }
            }
        }

        SearchSummaryPage(
            summaries = buildList {
                topItems.distinctBy { it.id }.takeIf { it.isNotEmpty() }
                    ?.let { add(com.pryvn.audiophile.code.api.innertube.pages.SearchSummary(title = "Top results", items = it)) }
                addAll(summaries)
            }
        )
    }

    suspend fun albumSongs(
        playlistId: String,
        album: AlbumItem? = null,
    ): Result<List<SongItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<JsonObject>()
        val page = AlbumPage.fromBrowseResponse(response) ?: return@runCatching emptyList()
        val songs = linkedMapOf<String, SongItem>()
        page.songs.forEach { songs.putIfAbsent(it.id, it) }

        var continuation = response["contents"]?.jsonObject
            ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("musicShelfRenderer")?.jsonObject
            ?.get("continuations")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("nextContinuationData")?.jsonObject
            ?.get("continuation")?.jsonPrimitive?.contentOrNull

        var requestCount = 0
        val maxRequests = 50
        while (continuation != null && requestCount < maxRequests) {
            requestCount++
            val contResponse = innerTube.browse(WEB_REMIX, continuation = continuation).body<JsonObject>()
            val contContents = contResponse["continuationContents"]?.jsonObject
                ?.get("musicShelfContinuation")?.jsonObject
            val newSongs = contContents?.get("contents")?.jsonArray
                ?.mapNotNull { it.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject }
                ?.mapNotNull { AlbumPage.parseSongItemFromListItem(it) }
                .orEmpty()
            val previousSize = songs.size
            newSongs.forEach { songs.putIfAbsent(it.id, it) }
            if (songs.size == previousSize) break
            continuation = contContents?.get("continuations")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("nextContinuationData")?.jsonObject
                ?.get("continuation")?.jsonPrimitive?.contentOrNull
        }
        songs.values.toList()
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<JsonObject>()
        ArtistItemsPage.fromBrowseResponse(response) ?: ArtistItemsPage("", emptyList(), null, ArtistItemsPage.ArtistItemsPageLayout.LIST)
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<JsonObject>()
        ArtistItemsPage.fromBrowseResponse(response) ?: ArtistItemsPage("", emptyList(), null, ArtistItemsPage.ArtistItemsPageLayout.LIST)
    }

    suspend fun playlistContinuation(
        continuation: String,
        playlistId: String? = null,
    ): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation, setLogin = true).body<JsonObject>()
        PlaylistContinuationPage.fromBrowseResponse(response, playlistId)
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore").body<JsonObject>()
        ExplorePage.fromBrowseResponse(response)
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        try {
            explore().getOrThrow().newReleaseAlbums
        } catch (t: Throwable) {
            emptyList()
        }
    }

    suspend fun moodAndGenres(): Result<List<ExplorePage.MoodAndGenre>> = runCatching {
        explore().getOrThrow().moodAndGenres
    }

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params).body<JsonObject>()
        BrowseResult.fromBrowseResponse(response)
    }

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage> = runCatching {
        val response = innerTube.browse(
            WEB_REMIX,
            browseId = "FEmusic_charts",
            params = "ggMGCgQIgAQ%3D",
            continuation = continuation,
        ).body<JsonObject>()
        ChartsPage.fromBrowseResponse(response)
    }

    suspend fun libraryContinuation(continuation: String): Result<LibraryContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation, setLogin = true).body<JsonObject>()
        LibraryContinuationPage.fromBrowseResponse(response)
    }

    suspend fun libraryRecentActivity(): Result<LibraryPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_liked_playlists", setLogin = true).body<JsonObject>()
        LibraryPage.fromBrowseResponse(response)
    }

    suspend fun musicHistory(): Result<HistoryPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_history", setLogin = true).body<JsonObject>()
        HistoryPage.fromSectionListContent(response)
    }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<JsonObject>()
        response["contents"]?.jsonObject
            ?.get("singleColumnBrowseResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("musicDescriptionShelfRenderer")?.jsonObject
            ?.get("description")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<JsonObject>()
        RelatedPage.fromBrowseResponse(response)
    }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> = runCatching {
        innerTube.getQueue(WEB_REMIX, videoIds, playlistId)
            .body<com.pryvn.audiophile.code.api.innertube.models.response.GetQueueResponse>()
            .queueDatas.mapNotNull { data ->
                val renderer = data.content?.playlistPanelVideoRenderer ?: return@mapNotNull null
                val title = renderer.title?.runs?.joinToString("") { it.text.orEmpty() }?.trim()
                    ?: return@mapNotNull null
                val videoId = renderer.videoId ?: return@mapNotNull null
                val artistName = renderer.shortBylineText?.runs?.firstOrNull()?.text
                val artistId = renderer.shortBylineText?.runs?.firstOrNull()?.navigationEndpoint?.watchEndpoint?.videoId
                val artists = if (artistName != null) {
                    listOf(com.pryvn.audiophile.code.api.innertube.models.YTItem.Artist(name = artistName, id = artistId))
                } else emptyList()
                val thumbnailUrl = renderer.thumbnail?.thumbnails?.lastOrNull()?.url
                val lengthText = renderer.lengthText?.simpleText
                var durationSeconds: Int? = null
                lengthText?.let { text ->
                    val parts = text.split(":")
                    durationSeconds = when (parts.size) {
                        2 -> parts[0].toIntOrNull()?.let { m -> parts[1].toIntOrNull()?.let { s -> m * 60 + s } }
                        3 -> parts[0].toIntOrNull()?.let { h -> parts[1].toIntOrNull()?.let { m -> parts[2].toIntOrNull()?.let { s -> h * 3600 + m * 60 + s } } }
                        else -> null
                    }
                }
                SongItem(
                    id = videoId,
                    title = title,
                    artists = artists,
                    thumbnailUrl = thumbnailUrl,
                    durationSeconds = durationSeconds,
                )
            }
    }

    suspend fun transcript(videoId: String): Result<String> = runCatching {
        val params = getTranscriptParams(videoId)
        val response = innerTube.getTranscript(WEB_REMIX, params)
            .body<com.pryvn.audiophile.code.api.innertube.models.response.GetTranscriptResponse>()
        response.actions?.firstOrNull()
            ?.updateEngagementPanelAction?.content
            ?.transcriptRenderer?.body?.transcriptBodyRenderer?.cueGroups
            ?.joinToString(separator = "\n") { group ->
                val time = group.transcriptCueGroupRenderer?.cues?.firstOrNull()
                    ?.transcriptCueRenderer?.startOffsetMs?.toLongOrNull() ?: 0L
                val text = group.transcriptCueGroupRenderer?.cues?.firstOrNull()
                    ?.transcriptCueRenderer?.cue?.simpleText?.trim('♪')?.trim() ?: ""
                "[%02d:%02d.%03d]$text".format(time / 60000, (time / 1000) % 60, time % 1000)
            } ?: ""
    }

    suspend fun visitorData(): Result<String> = runCatching {
        ensureVisitorData()
        visitorData ?: throw IllegalStateException("Failed to obtain visitor data")
    }

    suspend fun accountChannels(): Result<List<AccountChannel>> = runCatching {
        val response = innerTube.accountChannels(WEB_REMIX).body<JsonObject>()
        val accounts = response["contents"]?.jsonArray?.mapNotNull { account ->
            val renderer = account.jsonObject["accountRenderer"]?.jsonObject ?: return@mapNotNull null
            val name = renderer["accountName"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val channelId = renderer["channelHandle"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
            val thumbnail = renderer["accountPhoto"]?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull
            AccountChannel(name = name, channelId = channelId, thumbnailUrl = thumbnail)
        }.orEmpty()
        accounts
    }

    suspend fun getChannelId(browseId: String): String {
        artist(browseId).getOrNull()?.let { json ->
            val artistPage = ArtistPage.fromBrowseResponse(json)
            return artistPage?.artist?.channelId ?: ""
        }
        return ""
    }

    suspend fun likeVideo(videoId: String, like: Boolean): Result<Unit> = runCatching {
        if (like) {
            innerTube.likeVideo(WEB_REMIX, videoId)
        } else {
            innerTube.unlikeVideo(WEB_REMIX, videoId)
        }
    }

    suspend fun likePlaylist(playlistId: String, like: Boolean): Result<Unit> = runCatching {
        if (like) {
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        } else {
            innerTube.unlikeVideo(WEB_REMIX, playlistId)
        }
    }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit> = runCatching {
        if (subscribe) {
            innerTube.subscribeChannel(WEB_REMIX, channelId)
        } else {
            innerTube.unsubscribeChannel(WEB_REMIX, channelId)
        }
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String): Result<String> = runCatching {
        val response = innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId)
            .body<com.pryvn.audiophile.code.api.innertube.models.response.AddItemYouTubePlaylistResponse>()
        val result = response.playlistEditResults
            .firstOrNull { it.playlistEditVideoAddedResultData?.videoId == videoId }
            ?.playlistEditVideoAddedResultData
        require(result?.setVideoId?.isNotBlank() == true) { "Playlist edit did not confirm added video $videoId" }
        result.setVideoId
    }

    suspend fun addSongsToPlaylist(
        playlistId: String,
        videoIds: List<String>,
        batchSize: Int = 50,
        onProgress: (completedSongs: Int, totalSongs: Int) -> Unit = { _, _ -> },
    ): Result<List<String?>> = runCatching {
        if (videoIds.isEmpty()) return@runCatching emptyList()
        val setVideoIds = ArrayList<String?>(videoIds.size)
        val totalSongs = videoIds.size
        var completedSongs = 0
        onProgress(completedSongs, totalSongs)

        videoIds.chunked(batchSize).forEach { batch ->
            val batchResponse = runCatching {
                innerTube.addSongsToPlaylist(WEB_REMIX, playlistId, batch)
                    .body<com.pryvn.audiophile.code.api.innertube.models.response.AddItemYouTubePlaylistResponse>()
            }
            if (batchResponse.isSuccess) {
                val resultByVideoId = batchResponse.getOrThrow().playlistEditResults
                    .mapNotNull { it.playlistEditVideoAddedResultData }
                    .filter { it.setVideoId?.isNotBlank() == true }
                    .associateBy { it.videoId }
                batch.forEach { videoId ->
                    val setVideoId = resultByVideoId[videoId]?.setVideoId
                        ?: throw IllegalStateException("Playlist edit did not confirm added video $videoId")
                    setVideoIds += setVideoId
                    completedSongs += 1
                }
            } else if (batch.size == 1) {
                throw batchResponse.exceptionOrNull() ?: IllegalStateException("Playlist edit failed")
            } else {
                batch.forEach { videoId ->
                    val setVideoId = addToPlaylist(playlistId, videoId).getOrThrow()
                    setVideoIds += setVideoId
                    completedSongs += 1
                }
            }
            onProgress(completedSongs, totalSongs)
        }
        setVideoIds
    }

    suspend fun addPlaylistToPlaylist(playlistId: String, addPlaylistId: String): Result<Unit> = runCatching {
        innerTube.addToPlaylist(WEB_REMIX, playlistId, addPlaylistId)
    }

    suspend fun playlistEntrySetVideoIds(playlistId: String, videoId: String): Result<List<String>> = runCatching {
        val response = innerTube.next(WEB_REMIX, videoId, playlistId).body<JsonObject>()
        val contents = response["contents"]?.jsonObject
            ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
            ?.get("tabbedRenderer")?.jsonObject
            ?.get("watchNextTabbedResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("musicQueueRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("playlistPanelRenderer")?.jsonObject
            ?.get("contents")?.jsonArray.orEmpty()

        contents.mapNotNull { content ->
            content.jsonObject["playlistPanelVideoRenderer"]?.jsonObject
                ?.get("playlistSetVideoId")?.jsonPrimitive?.contentOrNull
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String): Result<Unit> = runCatching {
        innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId)
    }

    suspend fun moveSongPlaylist(playlistId: String, setVideoId: String, successorSetVideoId: String?): Result<Unit> = runCatching {
        innerTube.moveSongPlaylist(WEB_REMIX, playlistId, setVideoId, successorSetVideoId)
    }

    suspend fun createPlaylist(title: String, videoIds: List<String> = emptyList()): Result<String> = runCatching {
        val response = innerTube.createPlaylist(WEB_REMIX, title, videoIds)
            .body<com.pryvn.audiophile.code.api.innertube.models.response.CreatePlaylistResponse>()
        require(!response.playlistId.isNullOrBlank()) { "Created playlist but no ID returned" }
        response.playlistId
    }

    suspend fun renamePlaylist(playlistId: String, name: String): Result<Unit> = runCatching {
        innerTube.renamePlaylist(WEB_REMIX, playlistId, name)
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        innerTube.deletePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> = runCatching {
        val response = innerTube.getMediaInfo(WEB_REMIX, videoId).body<JsonObject>()
        val results = response["contents"]?.jsonObject
            ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
            ?.get("tabbedRenderer")?.jsonObject
            ?.get("watchNextTabbedResultsRenderer")?.jsonObject
            ?.get("tabs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("tabRenderer")?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray

        val primaryInfo = results?.firstOrNull()?.jsonObject
            ?.get("videoPrimaryInfoRenderer")?.jsonObject
        val secondaryInfo = results?.getOrNull(1)?.jsonObject
            ?.get("videoSecondaryInfoRenderer")?.jsonObject

        val title = primaryInfo?.get("title")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull

        val viewCount = primaryInfo?.get("viewCount")?.jsonObject
            ?.get("videoViewCountRenderer")?.jsonObject
            ?.get("viewCount")?.jsonObject
            ?.get("simpleText")?.jsonPrimitive?.contentOrNull
            ?.replace(Regex("[^0-9]"), "")?.toLongOrNull()

        val dateText = primaryInfo?.get("dateText")?.jsonObject
            ?.get("simpleText")?.jsonPrimitive?.contentOrNull

        val ownerRenderer = secondaryInfo?.get("owner")?.jsonObject
            ?.get("videoOwnerRenderer")?.jsonObject
        val author = ownerRenderer?.get("title")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
        val authorId = ownerRenderer?.get("title")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("navigationEndpoint")?.jsonObject
            ?.get("browseEndpoint")?.jsonObject
            ?.get("browseId")?.jsonPrimitive?.contentOrNull
        val authorThumbnail = ownerRenderer?.get("thumbnail")?.jsonObject
            ?.get("thumbnails")?.jsonArray
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull
        val subscribers = secondaryInfo?.get("owner")?.jsonObject
            ?.get("videoOwnerRenderer")?.jsonObject
            ?.get("subscriberCountText")?.jsonObject
            ?.get("simpleText")?.jsonPrimitive?.contentOrNull

        val description = secondaryInfo?.get("description")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: "" }
            ?.takeIf { it.isNotBlank() }

        MediaInfo(
            videoId = videoId,
            title = title,
            author = author,
            authorId = authorId,
            authorThumbnail = authorThumbnail,
            description = description,
            subscribers = subscribers,
            uploadDate = dateText,
            viewCount = viewCount,
        )
    }
}

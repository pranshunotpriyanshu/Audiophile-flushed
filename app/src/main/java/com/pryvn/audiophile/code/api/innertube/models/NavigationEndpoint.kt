package com.pryvn.audiophile.code.api.innertube.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NavigationEndpoint(
    @SerialName("watchEndpoint") val watchEndpoint: Endpoint.WatchEndpoint? = null,
    @SerialName("watchPlaylistEndpoint") val watchPlaylistEndpoint: Endpoint.WatchPlaylistEndpoint? = null,
    @SerialName("browseEndpoint") val browseEndpoint: Endpoint.BrowseEndpoint? = null,
    @SerialName("searchEndpoint") val searchEndpoint: Endpoint.SearchEndpoint? = null,
    @SerialName("queueAddEndpoint") val queueAddEndpoint: Endpoint.QueueAddEndpoint? = null,
    @SerialName("shareEntityEndpoint") val shareEntityEndpoint: Endpoint.ShareEntityEndpoint? = null,
) {
    val anyWatchEndpoint: Endpoint.WatchEndpoint?
        get() = watchEndpoint ?: Endpoint.WatchEndpoint(
            playlistId = watchPlaylistEndpoint?.playlistId,
            params = watchPlaylistEndpoint?.params,
            index = watchPlaylistEndpoint?.index,
        ).takeIf { watchPlaylistEndpoint != null }
}

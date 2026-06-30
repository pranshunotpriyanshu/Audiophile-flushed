package com.pryvn.audiophile.code.api.innertube.models.body

import com.pryvn.audiophile.code.api.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistBody(
    val context: Context,
    val title: String,
    val videoIds: List<String> = emptyList(),
)

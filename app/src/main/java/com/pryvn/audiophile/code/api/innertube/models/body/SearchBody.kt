package com.pryvn.audiophile.code.api.innertube.models.body

import com.pryvn.audiophile.code.api.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String? = null,
    val params: String? = null,
)

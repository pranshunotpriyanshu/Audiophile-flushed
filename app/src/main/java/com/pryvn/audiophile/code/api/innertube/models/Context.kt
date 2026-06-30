package com.pryvn.audiophile.code.api.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val client: Client,
    val user: User? = null,
    val request: Request? = null,
    val thirdParty: ThirdParty? = null,
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val clientId: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val platform: String? = null,
        val hl: String = "en",
        val gl: String = "US",
        val visitorData: String? = null,
    )

    @Serializable
    data class User(
        val lockedSafetyMode: Boolean = false,
        val onBehalfOfUser: String? = null,
    )

    @Serializable
    data class Request(
        val useSsl: Boolean = true,
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String? = null,
    )
}

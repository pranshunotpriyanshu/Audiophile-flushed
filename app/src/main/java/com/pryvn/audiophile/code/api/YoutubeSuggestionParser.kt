package com.pryvn.audiophile.code.api

import kotlinx.serialization.json.*

internal object YouTubeSuggestionParser {

    fun parseSuggestions(root: JsonObject): List<String> {
        val contents = root["contents"]?.jsonArray ?: return emptyList()
        val firstSection = contents.firstOrNull()?.jsonObject
            ?.get("searchSuggestionsSectionRenderer")?.jsonObject
            ?.get("contents")?.jsonArray ?: return emptyList()
        return firstSection.mapNotNull { item ->
            item.jsonObject["searchSuggestionRenderer"]?.jsonObject
                ?.get("suggestion")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.joinToString("") { run ->
                    run.jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
                }
        }
    }
}
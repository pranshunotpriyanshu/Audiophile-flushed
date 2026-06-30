package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class Runs(
    @SerializedName("runs") val runs: List<Run>? = null
)

data class Run(
    @SerializedName("text") val text: String? = null,
    @SerializedName("navigationEndpoint") val navigationEndpoint: NavigationEndpoint? = null
)

fun List<Run>?.splitBySeparator(): List<List<Run>> {
    if (this.isNullOrEmpty()) return emptyList()
    val result = mutableListOf<List<Run>>()
    var current = mutableListOf<Run>()
    for (run in this) {
        if (run.text == " • ") {
            if (current.isNotEmpty()) {
                result.add(current)
                current = mutableListOf()
            }
        } else {
            current.add(run)
        }
    }
    if (current.isNotEmpty()) {
        result.add(current)
    }
    return result
}

fun List<Run>?.oddElements(): List<Run> {
    if (this.isNullOrEmpty()) return emptyList()
    return this.filterIndexed { index, _ -> index % 2 == 1 }
}

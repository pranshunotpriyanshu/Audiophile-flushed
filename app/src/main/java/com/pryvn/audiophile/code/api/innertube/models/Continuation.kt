package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class Continuation(
    @SerializedName("continuation") val continuation: String? = null
)

data class ContinuationResponse(
    @SerializedName("continuation") val continuation: Continuation? = null
) {
    data class Continuation(
        @SerializedName("nextContinuationData") val nextContinuationData: NextContinuationData? = null
    ) {
        data class NextContinuationData(
            @SerializedName("continuation") val continuation: String? = null
        )
    }
}

fun List<ContinuationResponse?>?.getContinuation(): String? {
    return this?.firstOrNull()?.continuation?.nextContinuationData?.continuation
}

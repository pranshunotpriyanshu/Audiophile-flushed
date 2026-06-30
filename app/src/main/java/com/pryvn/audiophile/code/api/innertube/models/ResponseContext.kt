package com.pryvn.audiophile.code.api.innertube.models

import com.google.gson.annotations.SerializedName

data class ResponseContext(
    @SerializedName("visitorData") val visitorData: String? = null,
    @SerializedName("serviceTrackingParams") val serviceTrackingParams: List<ServiceTrackingParam>? = null
) {
    data class ServiceTrackingParam(
        @SerializedName("service") val service: String? = null,
        @SerializedName("params") val params: List<Param>? = null
    ) {
        data class Param(
            @SerializedName("key") val key: String? = null,
            @SerializedName("value") val value: String? = null
        )
    }
}

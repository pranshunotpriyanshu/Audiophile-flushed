package com.pryvn.audiophile.code.api.innertube.models.response

import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptResponse(
    val actions: List<Action>? = null,
) {
    @Serializable
    data class Action(
        val updateEngagementPanelAction: UpdateEngagementPanelAction? = null,
    )

    @Serializable
    data class UpdateEngagementPanelAction(
        val content: Content? = null,
    )

    @Serializable
    data class Content(
        val transcriptRenderer: TranscriptRenderer? = null,
    )

    @Serializable
    data class TranscriptRenderer(
        val body: Body? = null,
    )

    @Serializable
    data class Body(
        val transcriptBodyRenderer: TranscriptBodyRenderer? = null,
    )

    @Serializable
    data class TranscriptBodyRenderer(
        val cueGroups: List<CueGroup>? = null,
    )

    @Serializable
    data class CueGroup(
        val transcriptCueGroupRenderer: TranscriptCueGroupRenderer? = null,
    )

    @Serializable
    data class TranscriptCueGroupRenderer(
        val cues: List<Cue>? = null,
    )

    @Serializable
    data class Cue(
        val transcriptCueRenderer: TranscriptCueRenderer? = null,
    )

    @Serializable
    data class TranscriptCueRenderer(
        val startOffsetMs: String? = null,
        val cue: CueText? = null,
    )

    @Serializable
    data class CueText(
        val simpleText: String? = null,
    )
}

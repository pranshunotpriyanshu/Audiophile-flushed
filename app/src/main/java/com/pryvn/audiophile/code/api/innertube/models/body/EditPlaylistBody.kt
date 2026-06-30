package com.pryvn.audiophile.code.api.innertube.models.body

import com.pryvn.audiophile.code.api.innertube.models.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EditPlaylistBody(
    val context: Context,
    val playlistId: String,
    val actions: List<Action>,
)

@Serializable
data class Action(
    val action: String,
    val addedVideoId: String? = null,
    val addedFullListId: String? = null,
    val removedVideoId: String? = null,
    val setVideoId: String? = null,
    val movedSetVideoIdSuccessor: String? = null,
    val playlistName: String? = null,
) {
    companion object {
        fun AddVideoAction(addedVideoId: String) = Action(
            action = "ACTION_ADD_VIDEO",
            addedVideoId = addedVideoId,
        )
        fun AddPlaylistAction(addedFullListId: String) = Action(
            action = "ACTION_ADD_PLAYLIST",
            addedFullListId = addedFullListId,
        )
        fun RemoveVideoAction(removedVideoId: String, setVideoId: String) = Action(
            action = "ACTION_REMOVE_VIDEO",
            removedVideoId = removedVideoId,
            setVideoId = setVideoId,
        )
        fun MoveVideoAction(setVideoId: String, movedSetVideoIdSuccessor: String?) = Action(
            action = "ACTION_MOVE_VIDEO",
            setVideoId = setVideoId,
            movedSetVideoIdSuccessor = movedSetVideoIdSuccessor,
        )
        fun RenamePlaylistAction(playlistName: String) = Action(
            action = "ACTION_SET_PLAYLIST_NAME",
            playlistName = playlistName,
        )
    }
}
